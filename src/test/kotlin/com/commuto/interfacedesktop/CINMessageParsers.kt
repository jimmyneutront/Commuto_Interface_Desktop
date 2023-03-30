package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.key.keys.*
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

fun createPublicKeyAnnouncement(keyPair: KeyPair, offerId: ByteArray): String {
    //Setup encoder
    val encoder = Base64.getEncoder()

    //Create message object
    val message = SerializablePublicKeyAnnouncementMessage(
        encoder.encodeToString(keyPair.interfaceId),
        "pka",
        "",
        ""
    )

    //Create Base64-encoded string of public key in PKCS#1 bytes
    val pubKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

    //Create Base-64 encoded string of offer id
    val offerIdString = encoder.encodeToString(offerId)

    //Create payload object
    val payload = SerializablePublicKeyAnnouncementPayload(
        pubKeyString,
        offerIdString
    )

    //Create payload UTF-8 bytes and their Base64-encoded string
    val payloadString = Json.encodeToString(payload)
    val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

    //Set "payload" field of message
    message.payload = encoder.encodeToString(payloadUTF8Bytes)

    //Create signature of payload
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadUTF8Bytes)
    val signature = keyPair.sign(payloadDataHash)

    //Set signature field of message
    message.signature = encoder.encodeToString(signature)

    //Prepare and return message string
    val messageString = Json.encodeToString(message)
    return messageString
}

fun parsePublicKeyAnnouncement(messageString: String, makerInterfaceId: ByteArray, offerId: ByteArray): PublicKey? {
    //setup decoder
    val decoder = Base64.getDecoder()

    //Restore message object
    val message = try {
        Json.decodeFromString<SerializablePublicKeyAnnouncementMessage>(messageString)
    } catch (e: Exception) {
        return null
    }

    //Ensure that the message is a Public Key announcement message
    if (message.msgType != "pka") {
        return null
    }

    //Ensure that the sender is the maker
    try {
        if (!Arrays.equals(decoder.decode(message.sender), makerInterfaceId)) {
            return null
        }
    } catch (e: Exception) {
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

    //Ensure that the offer id in the PKA matches the offer in question
    try {
        if (!Arrays.equals(decoder.decode(payload.offerId), offerId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Re-create maker's public key
    val publicKey = try {
        PublicKey(decoder.decode(payload.pubKey))
    } catch (e: Exception) {
        return null
    }

    //Check that interface id of taker's key matches value in "sender" field of message
    try {
        if (!Arrays.equals(decoder.decode(message.sender), publicKey.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Create hash of payload
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)

    //Verify signature
    return try {
        if (publicKey.verifySignature(payloadDataHash, decoder.decode(message.signature))) {
            publicKey
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

fun createTakerInfoMessage(keyPair: KeyPair, makerPubKey: PublicKey, swapId: ByteArray,
                           paymentDetails: SerializablePaymentMethodDetails): String {
    //Setup encoder
    val encoder = Base64.getEncoder()

    //Create message object
    val message = SerializableTakerInfoMessage(
        encoder.encodeToString(keyPair.interfaceId),
        encoder.encodeToString(makerPubKey.interfaceId),
        "",
        "",
        "",
        ""
    )

    //Create Base64-encoded string of taker's public key in PKCS#1 bytes
    val pubKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

    //Create Base-64 encoded string of swap id
    val swapIdString = encoder.encodeToString(swapId)

    //Create payload object
    val payload = SerializableTakerInfoPayload(
        "takerInfo",
        pubKeyString,
        swapIdString,
        ""
    )

    //Create payment details UTF-8 bytes and their Base64-encoded string
    val paymentDetailsString = Json.encodeToString(paymentDetails)
    val paymentDetailsUTFBytes = paymentDetailsString.toByteArray(Charset.forName("UTF-8"))

    //Set "paymentDetails" field of payload
    payload.paymentDetails = encoder.encodeToString(paymentDetailsUTFBytes)

    //Create payload UTF-8 bytes and their Base64-encoded string
    val payloadString = Json.encodeToString(payload)
    val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

    //Generate a new symmetric key and initialization vector, and encrypt the payload bytes
    val symmetricKey = SymmetricKey()
    val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)

    //Set "payload" field of message
    message.payload = encoder.encodeToString(encryptedPayload.encryptedData)

    //Create signature of encrypted payload
    val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    val signature = keyPair.sign(encryptedPayloadHash)

    //Set signature field of message
    message.signature = encoder.encodeToString(signature)

    //Encrypt symmetric key and initialization vector with maker's public key
    val encryptedKey = makerPubKey.encrypt(symmetricKey.keyBytes)
    val encryptedIV = makerPubKey.encrypt(encryptedPayload.initializationVector)

    //Set "encryptedKey" and "encryptedIV" fields of message
    message.encryptedKey = encoder.encodeToString(encryptedKey)
    message.encryptedIV = encoder.encodeToString(encryptedIV)

    //Prepare and return message string
    val messageString = Json.encodeToString(message)
    return messageString
}

private val json = Json { ignoreUnknownKeys = true }

fun parseTakerInfoMessage(messageString: String, keyPair: KeyPair, takerInterfaceId: ByteArray,
                          swapId: ByteArray): Pair<PublicKey, SerializablePaymentMethodDetails>? {
    //setup decoder
    val decoder = Base64.getDecoder()

    //Restore message object
    val message = try {
        Json.decodeFromString<SerializableTakerInfoMessage>(messageString)
    } catch (e: Exception) {
        return null
    }

    //Ensure that the sender is the taker and the recipient is the maker
    try {
        if (!Arrays.equals(decoder.decode(message.sender), takerInterfaceId) ||
            !Arrays.equals(decoder.decode(message.recipient), keyPair.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Decrypt symmetric key, initialization vector and encrypted payload
    val symmetricKey = try {
        val encryptedKey = decoder.decode(message.encryptedKey)
        val decryptedKeyBytes = keyPair.decrypt(encryptedKey)
        SymmetricKey(decryptedKeyBytes)
    } catch (e: Exception) {
        return null
    }
    val encryptedPayloadBytes = try {
        decoder.decode(message.payload)
    } catch (e: Exception) {
        return null
    }
    val decryptedPayloadBytes = try {
        val encryptedIV = decoder.decode(message.encryptedIV)
        val decryptedIV = keyPair.decrypt(encryptedIV)
        val encryptedPayload = SymmetricallyEncryptedData(encryptedPayloadBytes, decryptedIV)
        symmetricKey.decrypt(encryptedPayload)
    } catch (e: Exception) {
        return null
    }

    //Restore payload object
    val payload = try {
        val payloadString = decryptedPayloadBytes.toString(Charset.forName("UTF-8"))
        Json.decodeFromString<SerializableTakerInfoPayload>(payloadString)
    } catch (e: Exception) {
        return null
    }

    //Ensure the message is a taker info message
    if (payload.msgType != "takerInfo") {
        return null
    }

    //Ensure that the swap id in the takerInfo message matches the swap in question
    try {
        if (!Arrays.equals(decoder.decode(payload.swapId), swapId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Re-create taker's public key
    val publicKey = try {
        PublicKey(decoder.decode(payload.pubKey))
    } catch (e: Exception) {
        return null
    }

    //Check that interface id of taker's key matches value in "sender" field of message
    try {
        if (!Arrays.equals(decoder.decode(message.sender), publicKey.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Create hash of encrypted payload
    val encryptedPayloadDataHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayloadBytes)

    //Verify signature
    try {
        if (!publicKey.verifySignature(encryptedPayloadDataHash, decoder.decode(message.signature))) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Restore payment details object
    /*
    TODO: In production, we should know the sort of payment info we are looking for, try to deserialize that type
    exactly, and return null if the payment info is for a different payment method
     */
    val paymentDetails = try {
        val paymentDetailsBytes = decoder.decode(payload.paymentDetails)
        val paymentDetailsString = paymentDetailsBytes.toString(Charset.forName("UTF-8"))
        /*
        Ignore unknown keys, because for some reason kotlinx.serialization can't decode the "type" key it put into
        the message in the first place
         */
        Json { ignoreUnknownKeys = true} .decodeFromString<SerializableUSD_SWIFT_Details>(paymentDetailsString)
    } catch (e: Exception) {
        return null
    }

    return Pair(publicKey, paymentDetails)
}

fun createMakerInfoMessage(keyPair: KeyPair, takerPubKey: PublicKey, swapId: ByteArray,
                           paymentDetails: SerializablePaymentMethodDetails): String {
    //Setup encoder
    val encoder = Base64.getEncoder()

    //Create message object
    val message = SerializableMakerInfoMessage(
        encoder.encodeToString(keyPair.interfaceId),
        encoder.encodeToString(takerPubKey.interfaceId),
        "",
        "",
        "",
        "",
    )

    //Create Base-64 encoded string of swap id
    val swapIdString = encoder.encodeToString(swapId)

    //Create payload object
    val payload = SerializableMakerInfoPayload(
        "makerInfo",
        swapIdString,
        ""
    )

    //Create payment details UTF-8 bytes and their Base64-encoded string
    val paymentDetailsString = Json.encodeToString(paymentDetails)
    val paymentDetailsUTF8Bytes = paymentDetailsString.toByteArray(Charset.forName("UTF-8"))

    //Set "paymentDetails" field of payload
    payload.paymentDetails = encoder.encodeToString(paymentDetailsUTF8Bytes)

    //Create payload UTF-8 bytes and their Base64-encoded string
    val payloadString = Json.encodeToString(payload)
    val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

    //Generate a new symmetric key and initialization vector, and encrypt the payload bytes
    val symmetricKey = SymmetricKey()
    val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)

    //Set "payload" field of message
    message.payload = encoder.encodeToString(encryptedPayload.encryptedData)

    //Create signature of encrypted payload
    val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
    val signature = keyPair.sign(encryptedPayloadHash)

    //Set signature field of message
    message.signature = encoder.encodeToString(signature)

    //Encrypt symmetric key and initialization vector with taker's public key
    val encryptedKey = takerPubKey.encrypt(symmetricKey.keyBytes)
    val encryptedIV = takerPubKey.encrypt(encryptedPayload.initializationVector)

    //Set "encryptedKey" and "encryptedIV" fields of message
    message.encryptedKey = encoder.encodeToString(encryptedKey)
    message.encryptedIV = encoder.encodeToString(encryptedIV)

    //Prepare and return message string
    val messageString = Json.encodeToString(message)
    return messageString
}

fun parseMakerInfoMessage(messageString: String, keyPair: KeyPair, makerPubKey: PublicKey, swapId: ByteArray):
        SerializablePaymentMethodDetails? {
    //Setup decoder
    val decoder = Base64.getDecoder()

    //Restore message object
    val message = try {
        Json.decodeFromString<SerializableMakerInfoMessage>(messageString)
    } catch (e: Exception) {
        return null
    }

    //Ensure that the sender is the maker and the recipient is the taker
    try {
        if (!Arrays.equals(decoder.decode(message.sender), makerPubKey.interfaceId) ||
            !Arrays.equals(decoder.decode(message.recipient), keyPair.interfaceId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Decode encrypted payload
    val encryptedPayloadBytes = try {
        decoder.decode(message.payload)
    } catch (e: Exception) {
        return null
    }

    //Create hash of encrypted payload
    val encryptedPayloadDataHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayloadBytes)

    //Verify signature
    try {
        if (!makerPubKey.verifySignature(encryptedPayloadDataHash, decoder.decode(message.signature))) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Decrypt symmetric key, initialization vector and encrypted payload
    val symmetricKey = try {
        val encryptedKey = decoder.decode(message.encryptedKey)
        val decryptedKeyBytes = keyPair.decrypt(encryptedKey)
        SymmetricKey(decryptedKeyBytes)
    } catch (e: Exception) {
        return null
    }
    val decryptedPayloadBytes = try {
        val encryptedIV = decoder.decode(message.encryptedIV)
        val decryptedIV = keyPair.decrypt(encryptedIV)
        val encryptedPayload = SymmetricallyEncryptedData(encryptedPayloadBytes, decryptedIV)
        symmetricKey.decrypt(encryptedPayload)
    } catch (e: Exception) {
        return null
    }

    //Restore payload object
    val payload = try {
        val payloadString = decryptedPayloadBytes.toString(Charset.forName("UTF-8"))
        Json.decodeFromString<SerializableMakerInfoPayload>(payloadString)
    } catch (e: Exception) {
        return null
    }

    //Ensure the message is a maker info message
    if (payload.msgType != "makerInfo") {
        return null
    }

    //Ensure that the swap id in the makerInfo message matches the swap in question
    try {
        if (!Arrays.equals(decoder.decode(payload.swapId), swapId)) {
            return null
        }
    } catch (e: Exception) {
        return null
    }

    //Restore payment details object
    /*
    TODO: In production, we should know the sort of payment info we are looking for, try to deserialize that type
    exactly, and return null if the payment info is for a different payment method
     */
    val paymentDetails = try {
        val paymentDetailsBytes = decoder.decode(payload.paymentDetails)
        val paymentDetailsString = paymentDetailsBytes.toString(Charset.forName("UTF-8"))
        /*
        Ignore unknown keys, because for some reason kotlinx.serialization can't decode the "type" key it put into
        the message in the first place
         */
        Json { ignoreUnknownKeys = true} .decodeFromString<SerializableUSD_SWIFT_Details>(paymentDetailsString)
    } catch (e: Exception) {
        return null
    }

    return paymentDetails
}