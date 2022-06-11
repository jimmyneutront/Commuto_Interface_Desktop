package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.keymanager.types.KeyPair
import com.commuto.interfacedesktop.keymanager.types.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

class P2PServiceTest {
    @Test
    fun testP2PService() = runBlocking {
        class TestOfferService : OfferMessageNotifiable {
            override fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
            }
        }
        val p2pService = P2PService(TestOfferService())
        p2pService.listenLoop()
    }

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

        class TestOfferService constructor(val expectedPKA: PublicKeyAnnouncement) : OfferMessageNotifiable {
            val publicKeyAnnouncementChannel = Channel<PublicKeyAnnouncement>()
            override fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
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
        val p2pService = P2PService(offerService)
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
            p2pService.mxClient.rooms.sendMessageEvent(
                roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                eventContent = RoomMessageEventContent
                    .TextMessageEventContent(publicKeyAnnouncementString)
            ).getOrThrow()
            withTimeout(30_000) {
                offerService.publicKeyAnnouncementChannel.receive()
            }
        }
    }
}