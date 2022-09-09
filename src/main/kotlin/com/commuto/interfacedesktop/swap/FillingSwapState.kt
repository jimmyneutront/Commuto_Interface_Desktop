package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently filling a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the swap filling process we are currently in.
 *
 * @property NONE Indicates that we are not currently filling the corresponding swap.
 * @property CHECKING Indicates that we are currently checking if we can fill the swap.
 * @property APPROVING Indicates that we are currently approving the token transfer to fill the corresponding swap.
 * @property FILLING Indicates that we are currently calling CommutoSwap's
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) function for the swap.
 * @property COMPLETED Indicates that we have filled the corresponding swap.
 * @property EXCEPTION Indicates that we encountered an exception while filling the corresponding swap.
 */
enum class FillingSwapState {
    NONE,
    CHECKING,
    APPROVING,
    FILLING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when(this) {
            // Note: This should not be used.
            NONE -> "Press Fill Swap to fill the swap"
            CHECKING -> "Checking that the swap can be filled..."
            APPROVING -> "Approving token transfer..."
            FILLING -> "Filling the swap..."
            COMPLETED -> "Swap successfully filled."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}