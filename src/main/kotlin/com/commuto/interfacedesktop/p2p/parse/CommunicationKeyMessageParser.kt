package com.commuto.interfacedesktop.p2p.parse

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.key.keys.SymmetricallyEncryptedData
import com.commuto.interfacedesktop.p2p.messages.CommunicationKeyMessage
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializableCommunicationKeyMessagePayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

/**
 * Attempts to restore a [CommunicationKeyMessage] from given optional [SerializableEncryptedMessage] using the
 * [PublicKey] of the sender and the [KeyPair] of the recipient.
 *
 * @param message An optional [SerializableEncryptedMessage] from which to try to restore a [CommunicationKeyMessage].
 * @param senderPublicKey The [PublicKey] of the sender of [message].
 * @param recipientKeyPair The [KeyPair] of the recipient of [message].
 *
 * @return An optional [CommunicationKeyMessage] that will be `null` if [message] does not contain a valid Communication
 * Key Message encrypted with the public key of [recipientKeyPair], and will be non-`null` if it does.
 */
fun parseCommunicationKeyMessage(
    message: SerializableEncryptedMessage?,
    senderPublicKey: PublicKey,
    recipientKeyPair: KeyPair
): CommunicationKeyMessage? {
    if (message == null) {
        return null
    }
    val decoder = Base64.getDecoder()
    try {
        if (!decoder.decode(message.recipient).contentEquals(recipientKeyPair.interfaceId)) {
            return null
        }
    } catch (exception: IllegalArgumentException) {
        return null
    }
    val symmetricKey = try {
        val encryptedKey = decoder.decode(message.encryptedKey)
        val decryptedKeyBytes = recipientKeyPair.decrypt(encryptedKey)
        SymmetricKey(decryptedKeyBytes)
    } catch (e: Exception) {
        return null
    }
    val decryptedIV = try {
        val encryptedIV = decoder.decode(message.encryptedIV)
        recipientKeyPair.decrypt(encryptedIV)
    } catch (e: Exception) {
        return null
    }
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
    val payload = try {
        val payloadString = String(bytes = decryptedPayloadBytes, charset = Charsets.UTF_8)
        Json.decodeFromString<SerializableCommunicationKeyMessagePayload>(payloadString)
    } catch (e: Exception) {
        return null
    }
    if (payload.msgType != "MCKAnnouncement") {
        return null
    }
    try {
        if (!decoder.decode(message.sender).contentEquals(senderPublicKey.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }
    val encryptedPayloadDataHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    try {
        if (!senderPublicKey.verifySignature(encryptedPayloadDataHash, decoder.decode(message.signature))) {
            return null
        }
    } catch (e: Exception) {
        return null
    }
    val communicationKey = try {
        val communicationKeyBytes = decoder.decode(payload.key)
        SymmetricKey(communicationKeyBytes)
    } catch (e: Exception) {
        return null
    }
    val swapID = try {
        UUID.fromString(payload.swapId)
    } catch (e: Exception) {
        return null
    }
    val chainID = try {
        BigInteger(payload.chainID)
    } catch (e: Exception) {
        return null
    }
    return CommunicationKeyMessage(
        messageType = payload.msgType,
        swapID = swapID,
        chainID = chainID,
        key = communicationKey
    )
}