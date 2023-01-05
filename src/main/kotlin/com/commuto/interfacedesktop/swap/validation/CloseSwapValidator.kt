package com.commuto.interfacedesktop.swap.validation

import com.commuto.interfacedesktop.swap.ClosingSwapState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapState

/**
 * Ensures that [swap] can be closed.
 *
 * This function ensures that:
 * - [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) is not already being
 * called for [swap].
 * - the next step in the swapping process for [swap] is the user calling
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent).
 *
 * @param swap The [Swap] to be validated for closing.
 *
 * @throws [SwapDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateSwapForClosing(swap: Swap) {
    if (swap.closingSwapState.value != ClosingSwapState.NONE &&
        swap.closingSwapState.value != ClosingSwapState.VALIDATING &&
        swap.closingSwapState.value != ClosingSwapState.EXCEPTION) {
        throw SwapDataValidationException("This Swap is already being closed.")
    }
    if (swap.state.value != SwapState.AWAITING_CLOSING) {
        throw SwapDataValidationException("This Swap cannot currently be closed.")
    }
}