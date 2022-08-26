package com.commuto.interfacedesktop.p2p.messages

import java.util.*

/**
 * This represents a Maker Information Message, as described in the
 * [Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 *
 * @property swapID The ID of the swap for which maker information is being sent.
 * @property settlementMethodDetails The maker's settlement method details.
 */
data class MakerInformationMessage(val swapID: UUID, val settlementMethodDetails: String)