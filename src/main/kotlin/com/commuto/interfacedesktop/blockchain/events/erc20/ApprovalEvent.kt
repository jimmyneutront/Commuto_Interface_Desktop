package com.commuto.interfacedesktop.blockchain.events.erc20

import java.math.BigInteger

/**
 * Represents an [Approval](https://ethereum.org/en/developers/docs/standards/tokens/erc-20/) event emitted by an ERC20
 * contract.
 *
 * @param owner The address of the token owner for which the allowance was set.
 * @param spender The address of the contract that has been given permission to spend some of [owner]'s balance.
 * @param amount The amount of [owner]'s balance that [spender] is allowed to spend.
 * @param purpose The reason for creating an allowance, such as opening an offer, or filling a swap.
 * @param transactionHash The hash of the transaction that emitted this event.
 * @param chainID The ID of the blockchain on which this event was emitted.
 */
class ApprovalEvent(
    val owner: String,
    val spender: String,
    val amount: BigInteger,
    val purpose: TokenTransferApprovalPurpose,
    transactionHash: String,
    val chainID: BigInteger
) {
    val transactionHash: String

    init {
        this.transactionHash = if (transactionHash.startsWith("0x")) {
            transactionHash.lowercase()
        } else {
            "0x${transactionHash.lowercase()}"
        }
    }

    companion object {
        /**
         * Creates an [ApprovalEvent] from a [CommutoApprovalEventResponse] and specified [purpose] and [chainID].
         *
         * @param event The [CommutoApprovalEventResponse] from which this attempts to create an [ApprovalEvent].
         * @param purpose A [TokenTransferApprovalPurpose] describing why the corresponding allowance was approved, such
         * as to open an offer or fill a swap.
         * @param chainID The ID of the blockchain on which this event was emitted.
         */
        fun fromEventResponse(
            event: CommutoApprovalEventResponse,
            purpose: TokenTransferApprovalPurpose,
            chainID: BigInteger
        ): ApprovalEvent {
            return ApprovalEvent(
                owner = event.owner,
                spender = event.spender,
                amount = event.amount,
                purpose = purpose,
                transactionHash = event.getEventResponseLog().transactionHash,
                chainID = chainID
            )
        }
    }

}