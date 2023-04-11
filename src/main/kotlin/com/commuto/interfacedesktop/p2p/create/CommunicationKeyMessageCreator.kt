package com.commuto.interfacedesktop.p2p.create

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializableCommunicationKeyMessagePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Communication Key message of the specified [messageType] using the supplied swap ID, blockchain ID,
 * communication key, recipient's public key, and sender's/first dispute agent's key pair according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 *
 * @param messageType One of "MCKAnnouncement" or "TCKAnnouncement", depending on whether [key] is the maker
 * communication key or taker communication key.
 * @param id The ID of the swap for which the user is sending a communication key.
 * @param chainID The ID of the blockchain on which the disputed swap for which the user is sending a communication key
 * exists.
 * @param recipientPublicKey The [PublicKey] of the recipient, with which the payload encryption key for this message
 * will be encrypted.
 * @param senderKeyPair The user's/first dispute agent's [KeyPair], with which this message will be signed.
 */
fun createCommunicationKeyMessage(
    messageType: String,
    id: String,
    chainID: String,
    key: String,
    recipientPublicKey: PublicKey,
    senderKeyPair: KeyPair
): String {
    val encoder = Base64.getEncoder()
    val payload = SerializableCommunicationKeyMessagePayload(
        msgType = messageType,
        swapId = id,
        chainID = chainID,
        key = key,
    )
    val payloadUTF8Bytes = Json.encodeToString(payload).toByteArray()
    val symmetricKey = SymmetricKey()
    val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)
    val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    val payloadSignature = senderKeyPair.sign(encryptedPayloadHash)
    val encryptedKey = recipientPublicKey.encrypt(symmetricKey.keyBytes)
    val encryptedIV = recipientPublicKey.encrypt(encryptedPayload.initializationVector)
    val message = SerializableEncryptedMessage(
        sender = encoder.encodeToString(senderKeyPair.interfaceId),
        recipient = encoder.encodeToString(recipientPublicKey.interfaceId),
        encryptedKey = encoder.encodeToString(encryptedKey),
        encryptedIV = encoder.encodeToString(encryptedIV),
        payload = encoder.encodeToString(encryptedPayload.encryptedData),
        signature = encoder.encodeToString(payloadSignature),
    )
    return Json.encodeToString(message)
}