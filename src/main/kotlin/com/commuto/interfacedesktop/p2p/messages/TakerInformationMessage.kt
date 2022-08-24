package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.key.keys.PublicKey
import java.util.*

/**
 * This represents a Taker Information Message, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 */
data class TakerInformationMessage(val swapID: UUID, val publicKey: PublicKey, val settlementMethodDetails: String)
