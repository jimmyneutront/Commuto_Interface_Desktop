package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.KMService
import com.commuto.interfacedesktop.kmService.kmTypes.KeyPair
import com.commuto.interfacedesktop.kmService.kmTypes.newSymmetricKey
import io.ktor.http.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.api.MatrixApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import kotlin.test.Test

class MatrixTests {

    @Test
    fun testGetUsersRooms() {
        runBlocking {
            val matrixRestClient = MatrixApiClient(
                baseUrl = Url("http://matrix.org"),
            ).apply { accessToken.value = "" }
            matrixRestClient.rooms.getJoinedRooms().getOrThrow().collect { value -> println(value) }
        }
    }

    @Test
    fun testSendingMessage() {
        runBlocking {
            val matrixRestClient = MatrixApiClient(
                baseUrl = Url("http://matrix.org"),
            ).apply { accessToken.value = "" }
            val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")
            val messageContent = "test_message"
            val eventId = matrixRestClient.rooms.sendMessageEvent(
                roomId = CINRoomId,
                eventContent = TextMessageEventContent(messageContent)
            ).getOrThrow()
            val retrievedEvent = matrixRestClient.rooms.getEvent(
                roomId = CINRoomId,
                eventId= eventId
            ).getOrThrow()
            assert(retrievedEvent is MessageEvent)
            assert(retrievedEvent.content.equals(TextMessageEventContent(messageContent)))
        }
    }
    /*
    @Test
    fun testSwapProcessMessages() {
        runBlocking {
            val direction = CommutoCoreInteraction.SwapDirection.BUY
            val role = CommutoCoreInteraction.ParticipantRole.TAKER

            //Setup DBService and KMService
            val driver = DatabaseDriverFactory()
            val dbService = DBService(driver)
            dbService.createTables()
            val kmService = KMService(dbService)

            //Create key pair and encoder
            val keyPair: KeyPair = kmService.generateKeyPair()
            val encoder = Base64.getEncoder()

            //Setup mxSession
            val matrixRestClient = MatrixApiClient(
                baseUrl = Url("http://matrix.org"),
            ).apply { accessToken.value = "" }
            val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")

            //Prepare this interface's payment method details JSON string
            val swiftDetails = USD_SWIFT_Details(
                "Jeff Roberts",
                "392649254057",
                "JEFROB38"
            )
            val ownPaymentDetails = Json.encodeToString(mapOf("USD-SWIFT" to swiftDetails)).toByteArray(Charset.forName("UTF-8"))

            if (role == CommutoCoreInteraction.ParticipantRole.MAKER) {
                //Create swap offer id
                val offerIdUUID = UUID.randomUUID()
                val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
                offerIdByteBuffer.putLong(offerIdUUID.mostSignificantBits)
                offerIdByteBuffer.putLong(offerIdUUID.leastSignificantBits)
                val offerId = offerIdByteBuffer.array()

                //Prepare public key announcement message
                val pkaPayload = PublicKeyAnnouncementPayload(
                    encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()),
                    encoder.encodeToString(offerId)
                )
                val payloadData = Json.encodeToString(pkaPayload).toByteArray(Charset.forName("UTF-8"))
                var payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadData)
                val payloadSignature = keyPair.sign(payloadDataHash)
                val pkaMessage = DecodedPublicKeyAnnouncement(
                    encoder.encodeToString(keyPair.interfaceId),
                    encoder.encodeToString(payloadData),
                    encoder.encodeToString(payloadSignature)
                )
                val pkaMessageString = Json.encodeToString(pkaMessage)

                //Send PKA message to CIN Matrix Room
                matrixRestClient.rooms.sendMessageEvent(
                    roomId = CINRoomId,
                    eventContent = RoomMessageEventContent.TextMessageEventContent(pkaMessageString)
                ).getOrThrow()

                //TODO: Listen to CIN Matrix Room for taker's TakerInfo message, and handle it

                //TODO: Save taker's public key locally

                //Prepare maker info message
                //TODO: Encrypt encryptedKey and encryptedIV with maker's public key
                val makerInfoMessageKey = newSymmetricKey()
                val symmetricallyEncryptedPayload = makerInfoMessageKey.encrypt(ownPaymentDetails)
                payloadDataHash = MessageDigest.getInstance("SHA-256").digest(symmetricallyEncryptedPayload.encryptedData)
                val makerInfoMsgObject = MakerInfoMessage(
                    encoder.encodeToString(keyPair.interfaceId),
                    encoder.encodeToString(takerInterfaceId),
                    "",
                    "",
                    encoder.encodeToString(symmetricallyEncryptedPayload.encryptedData),
                    encoder.encodeToString(keyPair.sign(payloadDataHash))
                )
                val makerInfoMsgString = Json.encodeToString(makerInfoMsgObject)

                //Send maker info message to CIN Matrix Room
                matrixRestClient.rooms.sendMessageEvent(
                    roomId = CINRoomId,
                    eventContent = RoomMessageEventContent.TextMessageEventContent(makerInfoMsgString)
                ).getOrThrow()
            } else if (role == CommutoCoreInteraction.ParticipantRole.TAKER) {

            }
        }
    }*/
}