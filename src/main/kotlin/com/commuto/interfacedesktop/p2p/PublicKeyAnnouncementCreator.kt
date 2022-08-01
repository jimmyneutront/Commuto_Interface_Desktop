package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Public Key Announcement for the given public key and offer ID according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @param offerID The ID of the offer for which the [PublicKey] is being announced.
 * @param keyPair The [KeyPair] containing the [PublicKey] to be announced.
 *
 * @return A JSON [String] that is the Public Key Announcement.
 */
fun createPublicKeyAnnouncement(offerID: UUID, keyPair: KeyPair): String {
    //Setup encoder
    val encoder = Base64.getEncoder()

    // Create Base64-encoded string of public key in PKCS#1 bytes
    val publicKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

    // Create a Base64-encoded string of the offer ID
    val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
    offerIDByteBuffer.putLong(offerID.mostSignificantBits)
    offerIDByteBuffer.putLong(offerID.leastSignificantBits)
    val offerIDByteArray = offerIDByteBuffer.array()
    val offerIDString = encoder.encodeToString(offerIDByteArray)

    // Create payload object
    val payloadObject = SerializablePublicKeyAnnouncementPayload(
        pubKey = publicKeyString,
        offerId = offerIDString
    )

    // Create payload UTF-8 bytes and their Base64-encoded string
    val payloadString = Json.encodeToString(payloadObject)
    val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

    // Create signature of payload hash
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadUTF8Bytes)
    val payloadDataSignature = keyPair.sign(payloadDataHash)

    // Create message object
    val message = SerializablePublicKeyAnnouncementMessage(
        sender = encoder.encodeToString(keyPair.interfaceId),
        msgType = "pka",
        payload = encoder.encodeToString(payloadUTF8Bytes),
        signature = encoder.encodeToString(payloadDataSignature)
    )

    // Prepare and return message string
    return Json.encodeToString(message) // messageString
}