package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.key.keys.PublicKey
import java.util.*

/**
 * This represents a Taker Information Message, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @property swapID The ID of the swap for which taker information is being sent.
 * @property publicKey The taker's public key.
 * @property settlementMethodDetails The taker's settlement method details, or `null` if no such data exists.
 */
data class TakerInformationMessage(val swapID: UUID, val publicKey: PublicKey, val settlementMethodDetails: String?)
