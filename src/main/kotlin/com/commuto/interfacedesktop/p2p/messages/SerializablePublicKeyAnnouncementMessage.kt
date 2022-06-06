package com.commuto.interfacedesktop.p2p.messages

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePublicKeyAnnouncementMessage(
    var sender: String,
    var msgType: String,
    var payload: String,
    var signature: String
)