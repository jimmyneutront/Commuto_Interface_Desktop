package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.create.createMakerInformationMessage
import com.commuto.interfacedesktop.p2p.create.createTakerInformationMessage
import com.commuto.interfacedesktop.p2p.messages.MakerInformationMessage
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.messages.TakerInformationMessage
import com.commuto.interfacedesktop.p2p.parse.parseMakerInformationMessage
import com.commuto.interfacedesktop.p2p.parse.parsePublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.parse.parsePublicKeyAnnouncementAsUserForDispute
import com.commuto.interfacedesktop.p2p.parse.parseTakerInformationMessage
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Tests for [P2PService].
 */
class P2PServiceTest {

    /**
     * Runs [P2PService.listenLoop] in the current coroutine context. This doesn't actually test
     * anything.
     */
    @Test
    fun testP2PService() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }
        val p2pService = P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        )
        p2pService.listenLoop()
    }

    /**
     * Tests [P2PService.listen] by ensuring it detects and handles public key announcements
     * properly.
     */
    @Test
    fun testListen() {

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val encoder = Base64.getEncoder()

        val keyPair = KeyPair()
        val publicKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())
        val offerId = UUID.randomUUID()
        val offerIdBytes = ByteBuffer.wrap(ByteArray(16))
            .putLong(offerId.mostSignificantBits)
            .putLong(offerId.leastSignificantBits).array()
        val offerIdString = encoder.encodeToString(offerIdBytes)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }

        class TestOfferService constructor(val expectedPKA: PublicKeyAnnouncement) : OfferMessageNotifiable {
            val publicKeyAnnouncementChannel = Channel<PublicKeyAnnouncement>()
            override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
                //TODO: compare interface ids
                if (message.id == expectedPKA.id) {
                    runBlocking {
                        publicKeyAnnouncementChannel.send(message)
                    }
                }
            }
        }

        val expectedPKA = PublicKeyAnnouncement(offerId, PublicKey(keyPair.keyPair.public))
        val offerService = TestOfferService(expectedPKA)
        val p2pService = P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = offerService,
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        )
        p2pService.listen()

        val publicKeyAnnouncementPayload = SerializablePublicKeyAnnouncementPayload(
            publicKeyString,
            offerIdString
        )
        val payloadString = Json.encodeToString(publicKeyAnnouncementPayload)
        val payloadBytes = payloadString.toByteArray(Charset.forName("UTF-8"))
        val encodedPayloadString = encoder.encodeToString(payloadBytes)

        val publicKeyAnnouncementMsg = SerializablePublicKeyAnnouncementMessage(
            encoder.encodeToString(keyPair.interfaceId),
            "pka",
            encodedPayloadString,
            ""
        )
        val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)
        publicKeyAnnouncementMsg.signature = encoder.encodeToString(keyPair.sign(payloadDataHash))
        val publicKeyAnnouncementString = Json.encodeToString(publicKeyAnnouncementMsg)
        runBlocking {
            mxClient.rooms.sendMessageEvent(
                roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                eventContent = RoomMessageEventContent
                    .TextMessageEventContent(publicKeyAnnouncementString)
            ).getOrThrow()
            withTimeout(40_000) {
                offerService.publicKeyAnnouncementChannel.receive()
            }
        }
    }

    /**
     * Tests [P2PService]'s error handling logic by ensuring that it handles unknown token errors
     * properly.
     */
    @Test
    fun testListenErrorHandling() {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }
        // We need this TestP2PExceptionHandler to test exception handling logic
        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            val exceptionChannel = Channel<Exception>()
            override fun handleP2PException(exception: Exception) {
                runBlocking {
                    exceptionChannel.send(exception)
                }
            }
        }
        val p2pExceptionHandler = TestP2PExceptionHandler()
        val p2pService = P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        )
        p2pService.listen()
        runBlocking {
            withTimeout(10_000) {
                val exception = p2pExceptionHandler.exceptionChannel.receive()
                assert(exception is MatrixServerException)
                assert((exception as MatrixServerException).errorResponse is
                        ErrorResponse.UnknownToken)
            }
        }
    }

    /**
     * Ensure that [P2PService.announcePublicKey] makes Public Key Announcements properly.
     */
    @Test
    fun testAnnouncePublicKey() {

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val p2pExceptionHandler = TestP2PExceptionHandler()

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = "not_a_real_token" }

        class TestP2PService : P2PService(
            p2pExceptionHandler,
            TestOfferMessageNotifiable(),
            TestSwapMessageNotifiable(),
            mxClient,
            keyManagerService
        ) {
            var receivedMessage: String? = null
            override suspend fun sendMessage(message: String) {
                receivedMessage = message
            }
        }
        val p2pService = TestP2PService()

        val keyPair = KeyPair()
        val offerID = UUID.randomUUID()

        runBlocking {
            p2pService.announcePublicKey(
                offerID = offerID,
                keyPair = keyPair,
            )
        }

        val createdPublicKeyAnnouncement = parsePublicKeyAnnouncement(p2pService.receivedMessage)
        assert(keyPair.interfaceId.contentEquals(createdPublicKeyAnnouncement!!.publicKey.interfaceId))
        assertEquals(offerID, createdPublicKeyAnnouncement.id)

    }

    /**
     * Ensure that [P2PService.sendTakerInformation] sends
     * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * s properly.
     */
    @Test
    fun testSendTakerInformation() {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val p2pExceptionHandler = TestP2PExceptionHandler()
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }
        class TestP2PService : P2PService(
            p2pExceptionHandler,
            TestOfferMessageNotifiable(),
            TestSwapMessageNotifiable(),
            mxClient,
            keyManagerService
        ) {
            var receivedMessage: String? = null
            override suspend fun sendMessage(message: String) {
                receivedMessage = message
            }
        }
        val p2pService = TestP2PService()

        // The key pair, the public key of which we use as the maker's key pair
        val makerKeyPair = KeyPair()
        // The taker's/user's key pair
        val takerKeyPair = KeyPair()

        val swapID = UUID.randomUUID()

        runBlocking {
            p2pService.sendTakerInformation(
                makerPublicKey = makerKeyPair.getPublicKey(),
                takerKeyPair = takerKeyPair,
                swapID = swapID,
                settlementMethodDetails = "some_settlement_method_details"
            )
        }

        val createdTakerInformationMessage = parseTakerInformationMessage(
            message = Json.decodeFromString<SerializableEncryptedMessage>(p2pService.receivedMessage!!),
            keyPair = makerKeyPair
        )
        assertEquals(swapID, createdTakerInformationMessage!!.swapID)
        assert(takerKeyPair.interfaceId.contentEquals(createdTakerInformationMessage.publicKey.interfaceId))
        assertEquals("some_settlement_method_details", createdTakerInformationMessage.settlementMethodDetails)

    }

    /**
     * Ensure that [P2PService.parseEvents] handles [TakerInformationMessage]s properly.
     */
    @Test
    fun testParseTakerInformationMessage() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // Since we are the maker, we want the maker's key pair in persistent storage.
        val makerKeyPair = keyManagerService.generateKeyPair(storeResult = true)
        // Since we are the maker, we do NOT want the taker's key pair in persistent storage.
        val takerKeyPair = KeyPair()

        val swapID = UUID.randomUUID()

        val p2pExceptionHandler = TestP2PExceptionHandler()
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }

        class TestSwapService: SwapMessageNotifiable {
            var message: TakerInformationMessage? = null
            override suspend fun handleTakerInformationMessage(message: TakerInformationMessage) {
                this.message = message
            }
            override suspend fun handleMakerInformationMessage(
                message: MakerInformationMessage,
                senderInterfaceID: ByteArray,
                recipientInterfaceID: ByteArray
            ) {}
        }
        val swapService = TestSwapService()

        val takerInformationMessageString = createTakerInformationMessage(
            makerPublicKey = makerKeyPair.getPublicKey(),
            takerKeyPair = takerKeyPair,
            swapID = swapID,
            settlementMethodDetails = "settlement_method_details"
        )

        val messageEventContent = RoomMessageEventContent.TextMessageEventContent(
            body = takerInformationMessageString
        )

        val messageEvent = Event.MessageEvent(
            content = messageEventContent,
            id = EventId(full = ""),
            sender = UserId(full = ""),
            roomId = RoomId(full = ""),
            originTimestamp = 0L,
        )

        val p2pService = P2PService(
            p2pExceptionHandler,
            TestOfferMessageNotifiable(),
            swapService,
            mxClient,
            keyManagerService
        )
        p2pService.parseEvents(events = listOf(messageEvent))

        assertEquals(swapID, swapService.message!!.swapID)
        assert(takerKeyPair.interfaceId.contentEquals(swapService.message!!.publicKey.interfaceId))
        assertEquals("settlement_method_details", swapService.message!!.settlementMethodDetails)
    }

    /**
     * Ensure that [P2PService.sendMakerInformation] sends
     * [Maker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)s
     * properly.
     */
    @Test
    fun testSendMakerInformation() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }
        class TestP2PService : P2PService(
            TestP2PExceptionHandler(),
            TestOfferMessageNotifiable(),
            TestSwapMessageNotifiable(),
            mxClient,
            keyManagerService
        ) {
            var receivedMessage: String? = null
            override suspend fun sendMessage(message: String) {
                receivedMessage = message
            }
        }
        val p2pService = TestP2PService()

        // The key pair, the public key of which we use as the taker's key pair
        val takerKeyPair = KeyPair()
        // The maker's/user's key pair
        val makerKeyPair = KeyPair()

        val swapID = UUID.randomUUID()

        // Send the information
        p2pService.sendMakerInformation(
            takerPublicKey = takerKeyPair.getPublicKey(),
            makerKeyPair = makerKeyPair,
            swapID = swapID,
            settlementMethodDetails = "some_payment_details"
        )
        // Parse sent information
        val createdMakerInformationMessage = parseMakerInformationMessage(
            message = Json.decodeFromString<SerializableEncryptedMessage>(p2pService.receivedMessage!!),
            keyPair = takerKeyPair,
            publicKey = makerKeyPair.getPublicKey(),
        )
        // Validate sent information
        assertEquals(swapID, createdMakerInformationMessage!!.swapID)
        assertEquals("some_payment_details", createdMakerInformationMessage.settlementMethodDetails)
    }

    /**
     * Ensure that [P2PService.parseEvents] handles [MakerInformationMessage]s properly.
     */
    @Test
    fun testParseMakerInformationMessage() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // Since we are the taker, we want the taker's key pair in persistent storage
        val takerKeyPair = keyManagerService.generateKeyPair(storeResult = true)
        // Since we are the taker, we do NOT want the maker's key pair in persistent storage
        val makerKeyPair = KeyPair()
        keyManagerService.storePublicKey(makerKeyPair.getPublicKey())

        val swapID = UUID.randomUUID()

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = System.getenv("MXKY") }

        class TestSwapService: SwapMessageNotifiable {
            var message: MakerInformationMessage? = null
            var senderInterfaceID: ByteArray? = null
            var recipientInterfaceID: ByteArray? = null
            override suspend fun handleTakerInformationMessage(message: TakerInformationMessage) {}
            override suspend fun handleMakerInformationMessage(
                message: MakerInformationMessage,
                senderInterfaceID: ByteArray,
                recipientInterfaceID: ByteArray
            ) {
                this.message = message
                this.senderInterfaceID = senderInterfaceID
                this.recipientInterfaceID = recipientInterfaceID
            }
        }
        val swapService = TestSwapService()

        val makerInformationMessageString = createMakerInformationMessage(
            takerPublicKey = takerKeyPair.getPublicKey(),
            makerKeyPair = makerKeyPair,
            swapID = swapID,
            settlementMethodDetails = "maker_settlement_method_details"
        )

        val messageEventContent = RoomMessageEventContent.TextMessageEventContent(
            body = makerInformationMessageString
        )

        val messageEvent = Event.MessageEvent(
            content = messageEventContent,
            id = EventId(full = ""),
            sender = UserId(full = ""),
            roomId = RoomId(full = ""),
            originTimestamp = 0L,
        )

        val p2pService = P2PService(
            TestP2PExceptionHandler(),
            TestOfferMessageNotifiable(),
            swapService,
            mxClient,
            keyManagerService
        )
        p2pService.parseEvents(events = listOf(messageEvent))

        assertEquals(swapID, swapService.message!!.swapID)
        assertEquals("maker_settlement_method_details", swapService.message!!.settlementMethodDetails)
        assert(makerKeyPair.interfaceId.contentEquals(swapService.senderInterfaceID))
        assert(takerKeyPair.interfaceId.contentEquals(swapService.recipientInterfaceID))

    }

    /**
     * Ensures that [P2PService.announcePublicKeyAsUserForDispute] functions properly.
     */
    @Test
    fun testAnnouncePublicKeyAsUserForDispute() {

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = "not_a_real_token" }

        class TestP2PService : P2PService(
            TestP2PExceptionHandler(),
            TestOfferMessageNotifiable(),
            TestSwapMessageNotifiable(),
            mxClient,
            keyManagerService
        ) {
            var receivedMessage: String? = null
            override suspend fun sendMessage(message: String) {
                receivedMessage = message
            }
        }
        val p2pService = TestP2PService()

        val keyPair = KeyPair()

        runBlocking {
            p2pService.announcePublicKeyAsUserForDispute(
                keyPair = keyPair
            )
        }

        val createdPublicKeyAnnouncement = parsePublicKeyAnnouncementAsUserForDispute(p2pService.receivedMessage)
        assert(keyPair.interfaceId.contentEquals(createdPublicKeyAnnouncement!!.publicKey.interfaceId))

    }

}