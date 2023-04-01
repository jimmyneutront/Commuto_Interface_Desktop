package com.commuto.interfacedesktop.p2p.serializable.messages

import kotlinx.serialization.Serializable

/**
 * A Public Key Announcement message, created by a dispute agent for a dispute in which they have been selected, as
 * described in the
 * [interface specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * This can be serialized to and deserialized from a JSON [String].
 *
 * @param sender The interface ID of the sender of this message, as a [String].
 * @param senderEthAddr The Ethereum address of the dispute agent sending this message.
 * @param msgType The type of this message (which should always be one of "disputeAgent0Pka", "disputeAgent1Pka", and
 * "disputeAgent2Pka"
 * @param payload The payload of this message.
 * @param signature A signature of the SHA-256 hash of the payload, signed with the private key corresponding to the
 * public key from which the interface ID in the [sender] property is derived.
 * @param signatureEth A signature of the SHA-256 hash of the payload, signed with the Ethereum key corresponding to the
 * address in the [senderEthAddr] field.
 */
@Serializable
data class SerializablePublicKeyAnnouncementAsAgentForDisputeMessage(
    val sender: String,
    val senderEthAddr: String,
    val msgType: String,
    val payload: String,
    val signature: String,
    val signatureEth: String
)
