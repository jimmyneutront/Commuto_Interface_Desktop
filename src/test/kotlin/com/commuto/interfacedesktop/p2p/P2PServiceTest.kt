package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
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
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }
        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            @Throws
            override fun handleP2PException(exception: Exception) {
                throw exception
            }
        }
        class TestOfferService : OfferMessageNotifiable {
            override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
            }
        }
        val p2pService = P2PService(TestP2PExceptionHandler(), TestOfferService(), mxClient)
        p2pService.listenLoop()
    }

    /**
     * Tests [P2PService.listen] by ensuring it detects and handles public key announcements
     * properly.
     */
    @Test
    fun testListen() {
        val encoder = Base64.getEncoder()

        val keyPair = KeyPair()
        val publicKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())
        val offerId = UUID.randomUUID()
        val offerIdBytes = ByteBuffer.wrap(ByteArray(16))
            .putLong(offerId.mostSignificantBits)
            .putLong(offerId.leastSignificantBits).array()
        val offerIdString = encoder.encodeToString(offerIdBytes)

        val httpClientFactory = { configBlock: (HttpClientConfig<*>.() -> Unit), ->
            HttpClient(configBlock).config {
                install(HttpTimeout) {
                    socketTimeoutMillis = 60_000
                }
            }
        }

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = httpClientFactory
        ).apply { accessToken.value = System.getenv("MXKY") }

        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            @Throws
            override fun handleP2PException(exception: Exception) {
                throw exception
            }
        }

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
        val p2pService = P2PService(TestP2PExceptionHandler(), offerService, mxClient)
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
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "not_a_real_token" }
        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            val exceptionChannel = Channel<Exception>()
            override fun handleP2PException(exception: Exception) {
                runBlocking {
                    exceptionChannel.send(exception)
                }
            }
        }
        val p2pExceptionHandler = TestP2PExceptionHandler()
        class TestOfferService : OfferMessageNotifiable {
            override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {}
        }
        val p2pService = P2PService(p2pExceptionHandler, TestOfferService(), mxClient)
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

        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            override fun handleP2PException(exception: Exception) {}
        }
        val p2pExceptionHandler = TestP2PExceptionHandler()
        class TestOfferService : OfferMessageNotifiable {
            override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {}
        }

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "not_a_real_token" }

        class TestP2PService : P2PService(p2pExceptionHandler, TestOfferService(), mxClient) {
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

}