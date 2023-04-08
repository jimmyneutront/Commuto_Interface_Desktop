package com.commuto.interfacedesktop.p2p.parse

import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncementAsUserForDispute
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementAsUserForDisputePayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Attempts to restore a [PublicKeyAnnouncementAsUserForDispute] from a given [String].
 *
 * @param messageString An optional [String] from which to try to restore a [PublicKeyAnnouncementAsUserForDispute].
 *
 * @return A [PublicKeyAnnouncementAsUserForDispute] if [messageString] contains a valid Public Key Announcement as the
 * user for a dispute, or `null` if it does not.
 */
fun parsePublicKeyAnnouncementAsUserForDispute(messageString: String?): PublicKeyAnnouncementAsUserForDispute? {
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
    // Ensure that the message is a Public Key Announcement as User For Dispute message
    if (message.msgType != "disputeUserPka") {
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
        Json.decodeFromString<SerializablePublicKeyAnnouncementAsUserForDisputePayload>(payloadString)
    } catch (e: Exception) {
        return null
    }

    val swapID = try {
        UUID.fromString(payload.id)
    } catch (e: Exception) {
        return null
    }

    val chainID = try {
        BigInteger(payload.chainID)
    } catch (e: Exception) {
        return null
    }

    // Re-create dispute raiser's public key
    val publicKey = try {
        PublicKey(decoder.decode(payload.pubKey))
    } catch (e: Exception) {
        return null
    }

    // Check that interface id of dispute raiser's key matches value in "sender" field of message
    if (!senderInterfaceID.contentEquals(publicKey.interfaceId)) {
        return null
    }

    // Create hash of payload
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)

    // Verify signature
    return try {
        when (publicKey.verifySignature(payloadDataHash, decoder.decode(message.signature))) {
            true -> PublicKeyAnnouncementAsUserForDispute(swapID, chainID, publicKey)
            false -> null
        }
    } catch (e: Exception) {
        null
    }
}