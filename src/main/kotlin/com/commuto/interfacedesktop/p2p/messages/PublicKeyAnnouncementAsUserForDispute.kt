package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.key.keys.PublicKey

/**
 * This represents a Public Key Announcement as a user for a dispute, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 *
 * @param publicKey The [PublicKey] that this [PublicKeyAnnouncementAsUserForDispute] is announcing.
 */
data class PublicKeyAnnouncementAsUserForDispute(val publicKey: PublicKey)