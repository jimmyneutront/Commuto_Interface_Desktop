package com.commuto.interfacedesktop.p2p.create

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementAsUserForDisputePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Public Key Announcement as a user for a dispute, given a public key.
 *
 * @param keyPair The [KeyPair] containing the public key to be announced.
 *
 * @return A JSON [String] that is the Public Key Announcement.
 */
fun createPublicKeyAnnouncementAsUserForDispute(
    keyPair: KeyPair,
): String {
    //Setup encoder
    val encoder = Base64.getEncoder()

    // Create Base64-encoded string of public key in PKCS#1 bytes
    val publicKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

    // Create payload object
    val payload = SerializablePublicKeyAnnouncementAsUserForDisputePayload(
        pubKey = publicKeyString,
    )

    // Create payload UTF-8 bytes
    val payloadUTF8Bytes = Json.encodeToString(payload).toByteArray(Charset.forName("UTF-8"))

    // Create signature of payload hash
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadUTF8Bytes)
    val payloadDataSignature = keyPair.sign(payloadDataHash)

    // Create message object
    val message = SerializablePublicKeyAnnouncementMessage(
        sender = encoder.encodeToString(keyPair.interfaceId),
        msgType = "disputeUserPka",
        payload = encoder.encodeToString(payloadUTF8Bytes),
        signature = encoder.encodeToString(payloadDataSignature)
    )

    // Prepare and return message string
    return Json.encodeToString(message)
}