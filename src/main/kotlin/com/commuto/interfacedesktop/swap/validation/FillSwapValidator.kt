package com.commuto.interfacedesktop.swap.validation

import com.commuto.interfacedesktop.swap.FillingSwapState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapRole
import com.commuto.interfacedesktop.swap.SwapState

/**
 * Ensures that [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) can be called for [swap].
 *
 * This function ensures that:
 * - [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) is not already being called for
 * [swap].
 * - The user of this interface is the maker and seller of [swap].
 * - The next step in the swapping process for [swap] is the user calling
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
 *
 * @param swap The [Swap] to be validated.
 *
 * @throws [SwapDataValidationException] if this is not able to ensure any of the conditions in the list above.
 * The descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so
 * that they can correct any problems.
 */
fun validateSwapForFilling(swap: Swap) {
    if (swap.fillingSwapState.value != FillingSwapState.NONE &&
        swap.fillingSwapState.value != FillingSwapState.VALIDATING &&
        swap.fillingSwapState.value != FillingSwapState.EXCEPTION) {
        throw SwapDataValidationException(desc = "This Swap is already being filled.")
    }
    if (swap.role != SwapRole.MAKER_AND_SELLER) {
        throw SwapDataValidationException("Only Maker-As-Seller Swaps can be filled")
    }
    if (swap.state.value != SwapState.AWAITING_FILLING) {
        throw SwapDataValidationException("This Swap cannot currently be filled")
    }
}