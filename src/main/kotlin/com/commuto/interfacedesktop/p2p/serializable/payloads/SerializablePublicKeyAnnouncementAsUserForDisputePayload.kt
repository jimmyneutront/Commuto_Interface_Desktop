package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Public Key Announcement payload](https://github.com/jimmyneutront/commuto-whitepaper/blob/968c3b9d0c044af535cb48e4aacdf3dc68c22370/commuto-interface-specification.txt#L256)
 * as a user for a dispute.
 *
 * @param id The ID of the disputed swap for which a public key is being announced, as a [String].
 * @param chainID The ID of the blockchain on which the disputed swap for which a public key is being announced exists,
 * as a [String].
 * @param pubKey The public key being announced, as a [String].
 */
@Serializable
data class SerializablePublicKeyAnnouncementAsUserForDisputePayload(
    val id: String,
    val chainID: String,
    val pubKey: String
)
