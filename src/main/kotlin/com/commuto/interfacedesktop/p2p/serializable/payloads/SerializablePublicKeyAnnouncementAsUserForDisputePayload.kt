package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Public Key Announcement payload](https://github.com/jimmyneutront/commuto-whitepaper/blob/968c3b9d0c044af535cb48e4aacdf3dc68c22370/commuto-interface-specification.txt#L256)
 * as a user for a dispute.
 *
 * @param pubKey The public key being announced, as a [String].
 */
@Serializable
data class SerializablePublicKeyAnnouncementAsUserForDisputePayload(val pubKey: String)
