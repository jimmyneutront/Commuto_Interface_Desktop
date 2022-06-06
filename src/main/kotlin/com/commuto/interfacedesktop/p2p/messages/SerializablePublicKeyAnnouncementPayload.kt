package com.commuto.interfacedesktop.p2p.messages

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePublicKeyAnnouncementPayload(
    val pubKey: String,
    val offerId: String
)