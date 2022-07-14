package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
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
import java.net.ConnectException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import javax.inject.Singleton

/**
 * The main Peer-to-Peer Service. It is responsible for listening to new messages in the Commuto
 * Interface Network Matrix room, parsing the messages, and then passing relevant messages to other
 * services as necessary.
 *
 * @constructor Creates a new [P2PService] with the specified [P2PExceptionNotifiable],
 * [OfferMessageNotifiable] and [MatrixClientServerApiClient].
 *
 * @property exceptionHandler An object to which [P2PService] will pass errors when they occur.
 * @property offerService An object to which [P2PService] will pass offer-related messages when they
 * occur.
 * @property mxClient A [MatrixClientServerApiClient] instance used to interact with a Matrix
 * Homeserver.
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
class P2PService constructor(private val exceptionHandler: P2PExceptionNotifiable,
                             private val offerService: OfferMessageNotifiable,
                             private val mxClient: MatrixClientServerApiClient) {

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
        listenJob = GlobalScope.launch {
            runLoop = true
            listenLoop()
        }
    }

    /**
     * Sets [runLoop] to false to prevent the listen loop from being executed again and cancels
     * [listenJob].
     */
    fun stopListening() {
        runLoop = false
        listenJob.cancel()
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
                val syncResponse = mxClient.sync.sync(timeout = 60_000)
                parseEvents(mxClient.rooms.getEvents(
                    roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                    from = lastNonEmptyBatchToken,
                    dir = GetEvents.Direction.FORWARDS,
                    limit = 1_000_000_000_000L
                ).getOrThrow().chunk!!)
                updateLastNonEmptyBatchToken(syncResponse.getOrThrow().nextBatch)
            } catch (e: Exception) {
                exceptionHandler.handleP2PException(e)
                if (e is ConnectException ||
                    (e as? MatrixServerException)?.errorResponse is ErrorResponse.UnknownToken ||
                    (e as? MatrixServerException)?.errorResponse is ErrorResponse.MissingToken) {
                    stopListening()
                }
            }
        }
    }

    /**
     * Parses a [List] of [Event.RoomEvent]s, filters out all non-text-message events, attempts to
     * create message objects from the content bodies of text message events, if present, and then
     * passes any offer-related message objects to [offerService].
     *
     * @param events A [List] of [Event.RoomEvent]s.
     */
    private suspend fun parseEvents(events: List<Event.RoomEvent<*>>) {
        val testMessageEvents = events.filterIsInstance<Event.MessageEvent<*>>().filter {
            it.content is RoomMessageEventContent.TextMessageEventContent
        }
        for (event in testMessageEvents) {
            parsePublicKeyAnnouncement((event.content as RoomMessageEventContent.
            TextMessageEventContent).body)?.let {
                offerService.handlePublicKeyAnnouncement(it)
            }
        }
    }

    /**
     * Attempts to restore a [PublicKeyAnnouncement] from a give [String].
     *
     * @param messageString An optional [String] from which to try to restore a
     * [PublicKeyAnnouncement]. If [messageString] is null, [parsePublicKeyAnnouncement] immediately
     * returns null.
     *
     * @return An optional [PublicKeyAnnouncement] that will be null if [messageString] does not
     * contain a valid public key announcement, and will be non-null if [messageString] does
     * contain a valid public key announcement.
     */
    private fun parsePublicKeyAnnouncement(messageString: String?): PublicKeyAnnouncement? {
        // Setup decoder
        val decoder = Base64.getDecoder()
        // Return null if no message string is given
        if (messageString == null) {
            return null
        }
        // Restore message object
        val message = try {
            Json.decodeFromString<SerializablePublicKeyAnnouncementMessage>(messageString)
        } catch (e: Exception) {
            return null
        }
        // Ensure that the message is a Public Key announcement message
        if (message.msgType != "pka") {
            return null
        }
        //Restore payload object
        val payloadBytes = try {
            decoder.decode(message.payload)
        } catch (e: Exception) {
            return null
        }
        val payload = try {
            val payloadString = payloadBytes.toString(Charset.forName("UTF-8"))
            Json.decodeFromString<SerializablePublicKeyAnnouncementPayload>(payloadString)
        } catch (e: Exception) {
            return null
        }
        val messageOfferId = try {
            val offerIdByteArray = decoder.decode(payload.offerId)
            val offerIdByteBuffer = ByteBuffer.wrap(offerIdByteArray)
            val mostSigBits = offerIdByteBuffer.long
            val leastSigBits = offerIdByteBuffer.long
            UUID(mostSigBits, leastSigBits)
        } catch (e: Exception) {
            return null
        }
        //Re-create maker's public key
        val publicKey = try {
            PublicKey(decoder.decode(payload.pubKey))
        } catch (e: Exception) {
            return null
        }
        //Create hash of payload
        val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)
        //Verify signature
        return try {
            when (publicKey.verifySignature(payloadDataHash, decoder.decode(message.signature))) {
                true -> PublicKeyAnnouncement(messageOfferId, publicKey)
                false -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}