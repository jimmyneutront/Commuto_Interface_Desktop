package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePublicKeyAnnouncementPayload(
    val pubKey: String,
    val offerId: String
)