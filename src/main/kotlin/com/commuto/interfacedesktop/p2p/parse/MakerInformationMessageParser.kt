package com.commuto.interfacedesktop.p2p.parse

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.key.keys.SymmetricallyEncryptedData
import com.commuto.interfacedesktop.p2p.messages.MakerInformationMessage
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializableMakerInformationMessagePayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*

/**
 * Attempts to restore a [MakerInformationMessage] from a given [SerializableEncryptedMessage] using a supplied
 * [KeyPair] and [PublicKey].
 *
 * @param message An optional [SerializableEncryptedMessage] from which to try to restore a [MakerInformationMessage].
 * @param keyPair The [KeyPair] with which this will attempt to decrypt the message's symmetric key and initialization
 * vector. The interface ID of this key pair should be that specified in the message's "recipient" field.
 * @param publicKey The [PublicKey] with which this will attempt to verify the message's signature. The interface ID of
 * this public key should be that specified in the message's "sender" field.
 *
 * @return An optional [MakerInformationMessage] that will be `null` if [message] does not contain a valid Maker
 * Information Message encrypted with the public key of [keyPair], and will be non-`null` if it does. If it does, the
 * [MakerInformationMessage.settlementMethodDetails] field will be `nil` if the `paymentDetails` field of `message`'s
 * payload contains an empty string, and will be non-`nil` otherwise.
 */
fun parseMakerInformationMessage(
    message: SerializableEncryptedMessage?,
    keyPair: KeyPair,
    publicKey: PublicKey
): MakerInformationMessage? {
    if (message == null) {
        return null
    }
    // Setup decoder
    val decoder = Base64.getDecoder()
    // Ensure that the recipient interface ID matches that of our key pair
    try {
        if (!decoder.decode(message.recipient).contentEquals(keyPair.interfaceId)) {
            return null
        }
    } catch (exception: IllegalArgumentException) {
        return null
    }
    // Attempt to decrypt symmetric key and initialization vector
    val symmetricKey = try {
        val encryptedKey = decoder.decode(message.encryptedKey)
        val decryptedKeyBytes = keyPair.decrypt(encryptedKey)
        SymmetricKey(decryptedKeyBytes)
    } catch (e: Exception) {
        return null
    }
    val decryptedIV = try {
        val encryptedIV = decoder.decode(message.encryptedIV)
        keyPair.decrypt(encryptedIV)
    } catch (e: Exception) {
        return null
    }
    // Decrypt the payload
    val encryptedPayload = try {
        SymmetricallyEncryptedData(
            data = decoder.decode(message.payload),
            iv = decryptedIV
        )
    } catch (e: Exception) {
        return null
    }
    val decryptedPayloadBytes = try {
        symmetricKey.decrypt(encryptedPayload)
    } catch (e: Exception) {
        return null
    }
    // Restore payload object
    val payload = try {
        val payloadString = String(bytes = decryptedPayloadBytes, charset = Charsets.UTF_8)
        Json.decodeFromString<SerializableMakerInformationMessagePayload>(payloadString)
    } catch (e: Exception) {
        return null
    }
    // Ensure that the message is a maker information message
    if (payload.msgType != "makerInfo") {
        return null
    }
    // Check that the interface ID of the maker's public key matches the value in the "sender" field of the message
    try {
        if (!decoder.decode(message.sender).contentEquals(publicKey.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }
    // Create a hash of the encrypted payload and verify the message's signature
    val encryptedPayloadDataHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    try {
        if (!publicKey.verifySignature(encryptedPayloadDataHash, decoder.decode(message.signature))) {
            return null
        }
    } catch (e: Exception) {
        return null
    }
    // Get settlement method details
    val settlementMethodDetails = payload.paymentDetails
    val optionalSettlementMethodDetails = if (settlementMethodDetails == "") {
        null
    } else {
        settlementMethodDetails
    }
    // Get swap ID
    val swapID = try {
        UUID.fromString(payload.swapId)
    } catch (e: Exception) {
        return null
    }
    return MakerInformationMessage(
        swapID = swapID,
        settlementMethodDetails = optionalSettlementMethodDetails,
    )
}