package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Public Key Announcement payload](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt#L58)
 *
 * @param pubKey The public key being announced, as a [String].
 * @param offerId The ID of the offer for which this Public Key Announcement is being made, as a
 * [String].
 */
@Serializable
data class SerializablePublicKeyAnnouncementPayload(
    val pubKey: String,
    val offerId: String
)