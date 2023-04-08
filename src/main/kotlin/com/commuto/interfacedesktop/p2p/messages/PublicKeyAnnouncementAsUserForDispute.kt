package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.key.keys.PublicKey
import java.math.BigInteger
import java.util.*

/**
 * This represents a Public Key Announcement as a user for a dispute, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 *
 * @param id The ID of the disputed swap for which a public key is being announced.
 * @param chainID The ID of the blockchain on which the disputed swap for which a public key is being announced exists.
 * @param publicKey The [PublicKey] that this [PublicKeyAnnouncementAsUserForDispute] is announcing.
 */
data class PublicKeyAnnouncementAsUserForDispute(val id: UUID, val chainID: BigInteger, val publicKey: PublicKey)