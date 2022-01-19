package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.kmService.kmTypes.PublicKey
import com.commuto.interfacedesktop.kmService.kmTypes.SymmetricKey
import kotlinx.serialization.Serializable

@Serializable
class SerializablePublicKeyAnnouncementMessage(
    var sender: String,
    var msgType: String,
    var payload: String,
    var signature: String
)

@Serializable
class SerializablePublicKeyAnnouncementPayload(
    val pubKey: String,
    val offerId: String
)

//TODO: Phase all of these out

@Serializable
class USD_SWIFT_Details(
    val Beneficiary: String,
    val Account: String,
    val BIC: String
)

@Serializable
class DecodedPublicKeyAnnouncement(
    val sender: String,
    val payload: String,
    val signature: String
)

@Serializable
class PublicKeyAnnouncementPayload(
    val pubKey: String,
    val offerId: String
)

@Serializable
class TakerInfoMessage(
    val sender: String,
    val recipient: String,
    val encryptedKey: String,
    val encryptedIV: String,
    val payload: String,
    val signature: String
)

@Serializable
class TakerInfoMessagePayload(
    val msgType: String,
    val pubKey: String,
    val swapId: String,
    val paymentDetails: String,
)

@Serializable
class MakerInfoMessage(
    val sender: String,
    val recipient: String,
    val encryptedKey: String,
    val encryptedIV: String,
    val payload: String,
    val signature: String
)

//These objects contain information restored from JSON messages
class TakerInfo(
    val sender: ByteArray,
    val recipient: ByteArray,
    val key: SymmetricKey,
    val encryptedIV: ByteArray,
    val msgType: String,
    val pubKey: PublicKey,
    val swapId: ByteArray,
    val paymentDetails: String,
    val signature: ByteArray
)