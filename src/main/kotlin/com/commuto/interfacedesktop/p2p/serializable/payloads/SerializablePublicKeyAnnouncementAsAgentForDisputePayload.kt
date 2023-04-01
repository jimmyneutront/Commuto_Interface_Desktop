package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A Public Key Announcement payload as an agent for a dispute.
 *
 * @param pubKey The public key being announced, as a [String].
 * @param swapId The ID of the swap being disputed, for which the sender of this message is a dispute agent.
 */
@Serializable
data class SerializablePublicKeyAnnouncementAsAgentForDisputePayload(val pubKey: String, val swapId: String)