package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Attempts to restore a [PublicKeyAnnouncement] from a given [String].
 *
 * @param messageString An optional [String] from which to try to restore a
 * [PublicKeyAnnouncement].
 *
 * @return A [PublicKeyAnnouncement] if [messageString] contains a valid
 * [Public Key Announcement](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt),
 * or `null` if it does not.
 */
fun parsePublicKeyAnnouncement(messageString: String?): PublicKeyAnnouncement? {
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

    // Get the interface ID of the sender
    val senderInterfaceID = try {
        decoder.decode(message.sender)
    } catch (e: Exception) {
        return null
    }

    // Restore payload object
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

    // Get the offer ID in the announcement
    val messageOfferId = try {
        val offerIdByteArray = decoder.decode(payload.offerId)
        val offerIdByteBuffer = ByteBuffer.wrap(offerIdByteArray)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        UUID(mostSigBits, leastSigBits)
    } catch (e: Exception) {
        return null
    }

    // Re-create maker's public key
    val publicKey = try {
        PublicKey(decoder.decode(payload.pubKey))
    } catch (e: Exception) {
        return null
    }

    // Check that interface id of maker's key matches value in "sender" field of message
    if (!senderInterfaceID.contentEquals(publicKey.interfaceId)) {
        return null
    }

    // Create hash of payload
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)

    // Verify signature
    return try {
        when (publicKey.verifySignature(payloadDataHash, decoder.decode(message.signature))) {
            true -> PublicKeyAnnouncement(messageOfferId, publicKey)
            false -> null
        }
    } catch (e: Exception) {
        null
    }
}