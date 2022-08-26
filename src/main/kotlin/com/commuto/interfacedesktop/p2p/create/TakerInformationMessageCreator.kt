package com.commuto.interfacedesktop.p2p.create

import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.newSymmetricKey
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializableEncryptedMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializableTakerInformationMessagePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Taker Information Message using the supplied maker's public key, taker's/user's key pair, swap ID and
 * taker's settlement method details according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @param makerPublicKey The [PublicKey] of the swap maker, to which the message will be sent.
 * @param takerKeyPair The [KeyPair] of the taker, with which the message will be signed.
 * @param swapID The ID of the swap for which the taker is sending information.
 * @param settlementMethodDetails The taker's settlement method details to be used for the swap.
 */
fun createTakerInformationMessage(
    makerPublicKey: PublicKey,
    takerKeyPair: KeyPair,
    swapID: UUID,
    settlementMethodDetails: String,
): String {
    // Setup encoder
    val encoder = Base64.getEncoder()

    // Create Base64-encoded string of the taker's (user's) public key in PKCS#1 bytes
    val takerPublicKeyString = encoder.encodeToString(takerKeyPair.pubKeyToPkcs1Bytes())

    // TODO: Note (in interface spec) that we are using a UUID string for the swap UUID
    // Create payload object
    val payload = SerializableTakerInformationMessagePayload(
        msgType = "takerInfo",
        pubKey = takerPublicKeyString,
        swapId = swapID.toString(),
        paymentDetails = settlementMethodDetails,
    )

    // Create payload UTF-8 bytes
    val payloadUTF8Bytes = Json.encodeToString(payload).toByteArray()

    // Generate a new AES-256 key and initialization vector, and encrypt the payload bytes
    val symmetricKey = newSymmetricKey()
    val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)

    // Create signature of encrypted payload
    val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    val payloadSignature = takerKeyPair.sign(encryptedPayloadHash)

    // Encrypt symmetric key and initialization vector with maker's public key
    val encryptedKey = makerPublicKey.encrypt(symmetricKey.keyBytes)
    val encryptedIV = makerPublicKey.encrypt(encryptedPayload.initializationVector)

    // Create message object
    val message = SerializableEncryptedMessage(
        sender = encoder.encodeToString(takerKeyPair.interfaceId),
        recipient = encoder.encodeToString(makerPublicKey.interfaceId),
        encryptedKey = encoder.encodeToString(encryptedKey),
        encryptedIV = encoder.encodeToString(encryptedIV),
        payload = encoder.encodeToString(encryptedPayload.encryptedData),
        signature = encoder.encodeToString(payloadSignature),
    )
    // Create message string
    return Json.encodeToString(message)
}