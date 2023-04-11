package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.key.keys.SymmetricKey
import java.math.BigInteger
import java.util.*

/**
 * This represents a Communication Key Message, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @property messageType One of "MCKAnnouncement" or "TCKAnnouncement", depending on whether [key] is the maker
 * communication key or taker communication key.
 * @property swapID The ID of the disputed swap for which a communication key is being sent.
 * @property chainID The ID of the blockchain on which the disputed swap for which a communication key is being sent
 * exists.
 * @property key The communication key being sent.
 */
data class CommunicationKeyMessage(
    val messageType: String,
    val swapID: UUID,
    val chainID: BigInteger,
    val key: SymmetricKey
)
