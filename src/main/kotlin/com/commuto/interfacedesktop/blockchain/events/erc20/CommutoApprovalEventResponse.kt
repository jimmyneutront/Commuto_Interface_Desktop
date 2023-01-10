package com.commuto.interfacedesktop.blockchain.events.erc20

import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger

/**
 * An object representing an [Approval](https://ethereum.org/en/developers/docs/standards/tokens/erc-20/) event emitted
 * by an ERC20 contract, that extends [BaseEventResponse].
 *
 * @param log The [Log] containing the information from which this event was made.
 * @param owner Corresponds to the `_owner` property of the corresponding Approval event.
 * @param spender Corresponds to the `_spender` property of the corresponding Approval event.
 * @param amount Corresponds to the `_value` property of the corresponding Approval event.
 * @param eventName A [String] containing the name of this event and describing the reason why it exists (such as to
 * open an offer, or fill a swap.)
 */
data class CommutoApprovalEventResponse(
    val log: Log,
    val owner: String,
    val spender: String,
    val amount: BigInteger,
    val eventName: String
): BaseEventResponse()