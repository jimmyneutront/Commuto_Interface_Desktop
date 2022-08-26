// Suppress warning in initializer
@file:Suppress("LeakingThis")

package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.p2p.create.createMakerInformationMessage
import com.commuto.interfacedesktop.p2p.create.createTakerInformationMessage
import com.commuto.interfacedesktop.p2p.parse.parseTakerInformationMessage
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.swap.SwapService
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Peer-to-Peer Service. It is responsible for listening to new messages in the Commuto
 * Interface Network Matrix room, parsing the messages, and then passing relevant messages to other
 * services as necessary.
 *
 * @constructor Creates a new [P2PService] with the specified [P2PExceptionNotifiable],
 * [OfferMessageNotifiable] and [MatrixClientServerApiClient].
 *
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property exceptionHandler An object to which [P2PService] will pass errors when they occur.
 * @property offerService An object that implements [OfferMessageNotifiable], to which this will pass offer-related
 * messages.
 * @property swapService An object that implements [SwapMessageNotifiable], to which this will pass swap-related
 * messages.
 * @property mxClient A [MatrixClientServerApiClient] instance used to interact with a Matrix
 * Homeserver.
 * @property keyManagerService A [KeyManagerService] from which this gets key pairs when attempting to decrypt encrypted
 * messages.
 * @property lastNonEmptyBatchToken The token at the end of the last batch of non-empty Matrix
 * events that was parsed. (The value specified here is that from the beginning of the Commuto
 * Interface Network testing room.) This should be updated every time a new batch of events is
 * parsed.
 * @property listenJob The coroutine [Job] in which [P2PService] listens for and parses new batches
 * of Matrix events.
 * @property runLoop Boolean that indicates whether [listenLoop] should continue to execute its
 * loop.
 */
@Singleton
open class P2PService constructor(
    private val exceptionHandler: P2PExceptionNotifiable,
    private val offerService: OfferMessageNotifiable,
    private val swapService: SwapMessageNotifiable,
    private val mxClient: MatrixClientServerApiClient,
    private val keyManagerService: KeyManagerService,
) {

    @Inject
    constructor(
        exceptionHandler: P2PExceptionNotifiable,
        offerService: OfferMessageNotifiable,
        swapService: SwapMessageNotifiable,
        keyManagerService: KeyManagerService,
    ): this(
        exceptionHandler = exceptionHandler,
        offerService = offerService,
        swapService = swapService,
        mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") },
        keyManagerService = keyManagerService
    )

    init {
        (offerService as? OfferService)?.setP2PService(this)
        (swapService as? SwapService)?.setP2PService(this)
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private var lastNonEmptyBatchToken =
        "t1-2607497254_757284974_11441483_1402797642_1423439559_3319206_507472245_4060289024_0"

    /**
     * Used to update [lastNonEmptyBatchToken]. Eventually, this method will store [newToken] in
     * persistent storage.
     *
     * @param newToken The batch token at the end of the most recently parsed batch of Matrix
     * events, to be set as [lastNonEmptyBatchToken].
     */
    private fun updateLastNonEmptyBatchToken(newToken: String) {
        lastNonEmptyBatchToken = newToken
    }

    private var listenJob: Job = Job()

    private var runLoop = true

    /**
     * Launches a new coroutine [Job] in [GlobalScope], the global coroutine scope, runs
     * [listenLoop] in this new [Job], and stores a reference to it in [listenJob].
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        logger.info("Starting listen loop in global coroutine scope")
        listenJob = GlobalScope.launch {
            runLoop = true
            listenLoop()
        }
        logger.info("Started listen loop in global coroutine scope")
    }

    /**
     * Sets [runLoop] to false to prevent the listen loop from being executed again and cancels
     * [listenJob].
     */
    fun stopListening() {
        logger.info("Stopping listen loop and canceling listen job")
        runLoop = false
        listenJob.cancel()
        logger.info("Stopped listen loop and canceled listen job")
    }

    /**
     * Executes the listening process in the current coroutine context as long as
     * [runLoop] is true.
     *
     * Listening Process:
     *
     * First, we sync with the Matrix Homeserver. Then we attempt to get all new Matrix events from
     * [lastNonEmptyBatchToken] to the present, with a limit of one trillion events. We then parse
     * these new events and then update the last non-empty batch token with the nextBatch token from
     * the sync response.
     *
     * If we encounter an [Exception], we pass it to [exceptionHandler]. Additionally, if the
     * exception is a [ConnectException], indicating that we are having problems communicating with
     * the homeserver, or if the exception is a [MatrixServerException], indicating that there is
     * something wrong with our Matrix API requests, then we stop listening.
     */
    suspend fun listenLoop() {
        while (true) {
            try {
                logger.info("Beginning iteration of listen loop")
                val syncResponse = mxClient.sync.sync(timeout = 60_000).getOrThrow()
                logger.info("Synced with Matrix homeserver, got nextBatchToken: ${syncResponse.nextBatch}")
                val eventsToParse = mxClient.rooms.getEvents(
                    roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                    from = lastNonEmptyBatchToken,
                    dir = GetEvents.Direction.FORWARDS,
                    limit = 1_000_000_000_000L
                ).getOrThrow().chunk!!
                logger.info("Got new messages from Matrix homeserver from lastNonEmptyBatchToken: " +
                        lastNonEmptyBatchToken)
                parseEvents(eventsToParse)
                logger.info("Finished parsing new events from chunk of size ${eventsToParse.size}")
                updateLastNonEmptyBatchToken(syncResponse.nextBatch)
                logger.info("Updated lastNonEmptyBatchToken with new token: ${syncResponse.nextBatch}")
            } catch (e: Exception) {
                logger.error("Got an exception during listen loop, calling exception handler", e)
                exceptionHandler.handleP2PException(e)
                if (e is ConnectException ||
                    (e as? MatrixServerException)?.errorResponse is ErrorResponse.UnknownToken ||
                    (e as? MatrixServerException)?.errorResponse is ErrorResponse.MissingToken) {
                    logger.error("Stopping listen loop for exception", e)
                    stopListening()
                }
            }
        }
    }

    /**
     * Parses a [List] of [Event.RoomEvent]s, filters out all non-text-message events, attempts to create message
     * objects from the content bodies of text message events, if present, and then passes any offer-related message
     * objects to [offerService] and any swap-related messages to [swapService].
     *
     * @param events A [List] of [Event.RoomEvent]s.
     */
    suspend fun parseEvents(events: List<Event.RoomEvent<*>>) {
        val textMessageEvents = events.filterIsInstance<Event.MessageEvent<*>>().filter {
            it.content is RoomMessageEventContent.TextMessageEventContent
        }
        logger.info("parseEvents: parsing ${textMessageEvents.size} text message events")
        for (event in textMessageEvents) {
            // Handle unencrypted messages
            val pka = parsePublicKeyAnnouncement((event.content as RoomMessageEventContent.
            TextMessageEventContent).body)
            if (pka != null) {
                logger.info("parseEvents: got Public Key Announcement message in event with Matrix event ID: " +
                        event.id.full)
                offerService.handlePublicKeyAnnouncement(pka)
            } else {
                /*
                If execution reaches this point, then we have already tried to get every possible unencrypted message
                from the event being handled. Therefore, we now try to get an encrypted message from the event: We
                attempt to cast the event content as text message event content, and then we attempt to create a
                SerializableEncryptedMessage from the text message event content body. Then we attempt to create an
                interface ID from the contents of the recipient field. Then we check keyManagerService to determine if
                we have a key pair with that interface ID. If we do, then we have determined that the event contains an
                encrypted message sent to us, and we attempt to parse it.
                 */
                val messageString = try {
                    (event.content as RoomMessageEventContent.TextMessageEventContent).body
                } catch (e: Exception) {
                    /*
                    If we can't can't get a message string from the room's content cast as TextMessageEventContent, then
                    we stop handling it and move on
                     */
                    break
                }
                val message = try {
                    Json.decodeFromString<SerializableEncryptedMessage>(messageString)
                } catch (e: Exception) {
                    /*
                    If we can't get a SerializableEncryptedMessage from the message, then we stop handling it and move
                    on
                     */
                    break
                }
                val recipientInterfaceID = try {
                    val decoder = Base64.getDecoder()
                    decoder.decode(message.recipient)
                } catch (e: Exception) {
                    /*
                    If we can't create a recipient interface ID from the contents of the "recipient" field, then we stop
                    handling it and move on
                     */
                    break
                }
                /*
                If we don't have a key pair with the interface ID specified in the "recipient" field, then we don't
                have the private key necessary to decrypt the message, (meaning we aren't the intended recipient) so
                we stop handling it and move on
                */
                val recipientKeyPair = keyManagerService.getKeyPair(recipientInterfaceID)
                    ?:
                    break
                val takerInformationMessage = parseTakerInformationMessage(
                    messageString = messageString,
                    keyPair = recipientKeyPair
                )
                if (takerInformationMessage != null) {
                    logger.info("parseEvents: got Taker Information Message in event with Matrix event ID: " +
                            event.id.full)
                    swapService.handleTakerInformationMessage(takerInformationMessage)
                }
            }
        }
    }

    /**
     * Sends the given message [String] in the Commuto Interface Network Test Room.
     *
     * @param message The [String] to send in the Commuto Interface Network Test Room.
     */
    open suspend fun sendMessage(message: String) {
        logger.info("sendMessage: sending $message")
        val result = mxClient.rooms.sendMessageEvent(
            roomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org"),
            eventContent = RoomMessageEventContent.TextMessageEventContent(message),
        ).getOrElse {
            logger.error("sendMessage: got exception", it)
            throw it
        }
        logger.info("sendMessage: success; ID: $result")
    }

    /**
     * Creates a
     * [Public Key Announcement](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * using the given offer ID and key pair and sends it using the [sendMessage] function.
     *
     * @param offerID The ID of the offer for which the Public Key Announcement is being made.
     * @param keyPair The key pair containing the public key to be announced.
     */
    open suspend fun announcePublicKey(offerID: UUID, keyPair: KeyPair) {
        val encoder = Base64.getEncoder()
        logger.info("announcePublicKey: creating for offer $offerID and key pair with interface ID " +
                encoder.encodeToString(keyPair.interfaceId)
        )
        val announcement = createPublicKeyAnnouncement(offerID = offerID, keyPair = keyPair)
        logger.info("announcePublicKey: sending announcement for offer $offerID")
        sendMessage(announcement)
    }

    /**
     * Creates a
     * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * using the supplied maker's public key, taker's/user's key pair, swap ID and taker's payment details, and sends it
     * using the [sendMessage] function.
     *
     * @param makerPublicKey The public key of the swap maker, to whom information is being sent.
     * @param takerKeyPair The taker's/user's key pair, which will be used to sign this message.
     * @param swapID The ID of the swap for which information is being sent.
     * @param settlementMethodDetails The settlement method details being sent.
     */
    open suspend fun sendTakerInformation(
        makerPublicKey: PublicKey,
        takerKeyPair: KeyPair,
        swapID: UUID,
        settlementMethodDetails: String,
    ) {
        logger.info("sendTakerInformation: creating for $swapID")
        val messageString = createTakerInformationMessage(
            makerPublicKey = makerPublicKey,
            takerKeyPair = takerKeyPair,
            swapID = swapID,
            settlementMethodDetails = settlementMethodDetails
        )
        logger.info("sendTakerInformation: sending for $swapID")
        sendMessage(messageString)
    }

    /**
     * Creates a
     * [Maker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * using the supplied taker's public key, maker's/user's key pair, swap ID and maker's payment details, and sends it
     * using the [sendMessage] function.
     *
     * @param takerPublicKey The public key of the swap taker, to whom information is being sent.
     * @param makerKeyPair The maker's/user's key pair, which will be used to sign this message.
     * @param swapID The ID of the swap for which information is being sent.
     * @param settlementMethodDetails The settlement method details being sent.
     */
    open suspend fun sendMakerInformation(
        takerPublicKey: PublicKey,
        makerKeyPair: KeyPair,
        swapID: UUID,
        settlementMethodDetails: String,
    ) {
        logger.info("sendMakerInformation: creating for $swapID")
        val messageString = createMakerInformationMessage(
            takerPublicKey = takerPublicKey,
            makerKeyPair = makerKeyPair,
            swapID = swapID,
            settlementMethodDetails = settlementMethodDetails
        )
        logger.info("sendMakerInformation: sending for $swapID")
        sendMessage(messageString)
    }

}