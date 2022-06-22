package com.commuto.interfacedesktop.p2p.serializable.messages

import kotlinx.serialization.Serializable

/**
 * A [Public Key Announcement message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt#L51)
 * that can be serialized to and deserialized from a JSON [String].
 *
 * @param sender The interface ID of the sender of this message, as a [String].
 * @param msgType The type of this message (which should always be "pka"), as a [String].
 * @param payload The payload of this message, as a [String].
 * @param signature The signature of the payload, signed with the private key corresponding to the
 * public key from which the interface ID in the [sender] property is derived.
 */
@Serializable
data class SerializablePublicKeyAnnouncementMessage(
    var sender: String,
    var msgType: String,
    var payload: String,
    var signature: String
)