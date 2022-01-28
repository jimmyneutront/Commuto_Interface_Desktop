package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.kmService.kmTypes.PublicKey
import com.commuto.interfacedesktop.kmService.kmTypes.SymmetricKey
import kotlinx.serialization.SerialName
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

@Serializable
class SerializableTakerInfoMessage(
    val sender: String,
    val recipient: String,
    var encryptedKey: String,
    var encryptedIV: String,
    var payload: String,
    var signature: String
)

@Serializable
class SerializableTakerInfoPayload(
    val msgType: String,
    val pubKey: String,
    val swapId: String,
    var paymentDetails: String
)

@Serializable
sealed class SerializablePaymentMethodDetails {
    abstract val name: String
}

@Serializable
data class SerializableUSD_SWIFT_Details(
    override val name: String,
    val beneficiary: String,
    val account: String,
    val bic: String,
) : SerializablePaymentMethodDetails()

@Serializable
class SerializableMakerInfoMessage(
    val sender: String,
    val recipient: String,
    var encryptedKey: String,
    var encryptedIV: String,
    var payload: String,
    var signature: String,
)

@Serializable
class SerializableMakerInfoPayload(
    val msgType: String,
    val swapId: String,
    var paymentDetails: String
)

//TODO: Phase all of these out

@Serializable
class USD_SWIFT_Details_old(
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