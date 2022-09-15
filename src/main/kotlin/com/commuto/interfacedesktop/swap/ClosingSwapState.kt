package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently closing a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the swap closing process we are currently in.
 *
 * @property NONE Indicates that we are not currently closing the corresponding swap.
 * @property CHECKING Indicates that we are currently checking if we can close the corresponding swap.
 * @property CLOSING Indicates that we are currently calling CommutoSwap's
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) function for the swap.
 * @property COMPLETED Indicates that we have closed the corresponding swap.
 * @property EXCEPTION Indicates that we encountered an exception while closing the corresponding swap.
 * @property description A human readable string describing the current state.
 */
enum class ClosingSwapState {
    NONE,
    CHECKING,
    CLOSING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when (this) {
            // Note: This should not be used.
            NONE -> "Press Close Swap to close the swap"
            CHECKING -> "Checking that swap can be closed..."
            CLOSING -> "Closing swap..."
            COMPLETED -> "Successfully closed swap."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }

}