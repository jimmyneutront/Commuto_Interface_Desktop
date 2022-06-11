package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.keymanager.types.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import javax.inject.Singleton

@Singleton
class P2PService constructor(private val offerService: OfferMessageNotifiable) {

    // Matrix REST client
    val mxClient = MatrixClientServerApiClient(
        baseUrl = Url("http://matrix.org")
    ).apply { accessToken.value = System.getenv("MXKY")}

    /*
     The token at the end of the last batch of non-empty messages (this token is that from the
     beginning of the Commuto Interface network test room)
      */
    private var lastNonEmptyBatchToken =
        "t1-2607497254_757284974_11441483_1402797642_1423439559_3319206_507472245_4060289024_0"

    private fun updateLastNonEmptyBatchToken(newToken: String) {
        lastNonEmptyBatchToken = newToken
    }

    // The job in which P2PService listens for messages
    private var listenJob: Job = Job()

    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        listenJob = GlobalScope.launch {
            listenLoop()
        }
    }

    fun stopListening() {
        listenJob.cancel()
    }

    suspend fun listenLoop() {
        while (true) {
            val syncResponse = mxClient.sync.sync(timeout = 60_000)
            parseEvents(mxClient.rooms.getEvents(
                roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                from = lastNonEmptyBatchToken,
                dir = GetEvents.Direction.FORWARDS,
                limit = 1_000_000_000_000L
            ).getOrThrow().chunk!!)
            updateLastNonEmptyBatchToken(syncResponse.getOrThrow().nextBatch)
        }
    }

    private fun parseEvents(events: List<Event.RoomEvent<*>>) {
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