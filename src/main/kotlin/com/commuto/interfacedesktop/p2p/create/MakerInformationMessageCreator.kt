package com.commuto.interfacedesktop.p2p.create

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.newSymmetricKey
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializableMakerInformationMessagePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Maker Information Message using the supplied taker's public key, maker's/user's key pair, swap ID and
 * maker's settlement method details according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @param takerPublicKey The [PublicKey] of the swap taker, to which the message will be sent.
 * @param makerKeyPair The [KeyPair] of the maker, with which the message will be signed.
 * @param swapID The ID of the swap for which the maker is sending information.
 * @param settlementMethodDetails The maker's settlement method details to be used for the swap, as an optional string.
 * If this is `null`, the `paymentDetails` field of the payload of the resulting message will be the empty string.
 */
fun createMakerInformationMessage(
    takerPublicKey: PublicKey,
    makerKeyPair: KeyPair,
    swapID: UUID,
    settlementMethodDetails: String?
): String {
    // Setup encoder
    val encoder = Base64.getEncoder()

    // TODO: Note (in interface spec) that we are using a UUID string for the swap UUID
    // Create payload object
    val payload = SerializableMakerInformationMessagePayload(
        msgType = "makerInfo",
        swapId = swapID.toString(),
        paymentDetails = settlementMethodDetails ?: "",
    )

    // Create payload UTF-8 bytes
    val payloadUTF8Bytes = Json.encodeToString(payload).toByteArray()

    // Generate a new AES-256 key and initialization vector, and encrypt the payload bytes
    val symmetricKey = newSymmetricKey()
    val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)

    // Create signature of encrypted payload
    val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    val payloadSignature = makerKeyPair.sign(encryptedPayloadHash)

    // Encrypt symmetric key and initialization vector with taker's public key
    val encryptedKey = takerPublicKey.encrypt(symmetricKey.keyBytes)
    val encryptedIV = takerPublicKey.encrypt(encryptedPayload.initializationVector)

    // Create message object
    val message = SerializableEncryptedMessage(
        sender = encoder.encodeToString(makerKeyPair.interfaceId),
        recipient = encoder.encodeToString(takerPublicKey.interfaceId),
        encryptedKey = encoder.encodeToString(encryptedKey),
        encryptedIV = encoder.encodeToString(encryptedIV),
        payload = encoder.encodeToString(encryptedPayload.encryptedData),
        signature = encoder.encodeToString(payloadSignature),
    )
    // Create message string
    return Json.encodeToString(message)
}