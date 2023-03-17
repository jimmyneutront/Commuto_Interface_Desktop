package com.commuto.interfacedesktop.dispute.validation

import com.commuto.interfacedesktop.dispute.DisputeState
import com.commuto.interfacedesktop.dispute.RaisingDisputeState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapState

/**
 * Ensures that [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) can be
 * called for [swap].
 *
 * This function ensures that:
 * - [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) is not already being
 * called for [swap].
 * - The swap has not already been disputed.
 * - The swap is in a state that allows raising a dispute.
 *
 * @param swap The [Swap] to be validated.
 *
 * @throws [DisputeValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateSwapForRaisingDispute(swap: Swap) {
    if (swap.raisingDisputeState.value != RaisingDisputeState.NONE &&
        swap.raisingDisputeState.value != RaisingDisputeState.VALIDATING &&
        swap.raisingDisputeState.value != RaisingDisputeState.EXCEPTION) {
        throw DisputeValidationException(desc = "A dispute is already being raised for this Swap.")
    }
    if (swap.disputeState.value != DisputeState.NONE) {
        throw DisputeValidationException(desc = "This Swap has already been disputed.")
    }
    if (swap.state.value == SwapState.AWAITING_CLOSING ||
        swap.state.value == SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST ||
        swap.state.value == SwapState.CLOSED ||
        swap.hasBuyerClosed ||
        swap.hasSellerClosed) {
        throw DisputeValidationException(desc = "This Swap cannot be disputed.")
    }
}