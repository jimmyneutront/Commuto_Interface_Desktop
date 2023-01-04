package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently closing a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the swap closing process we are currently in.
 *
 * @property NONE Indicates that we are not currently closing the corresponding swap.
 * @property VALIDATING Indicates that we are currently checking if we can call
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for
 * the swap.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will call
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for
 * the corresponding swap.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will call
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for
 * the corresponding swap, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have called
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for
 * the corresponding swap, and that the transaction to do so has been confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while calling
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for
 * the corresponding swap.
 * @property asString Returns a [String] corresponding to a particular case of [ReportingPaymentSentState]. This is
 * primarily used for database storage.
 * @property description A human readable string describing the current state.
 */
enum class ClosingSwapState {
    NONE,
    VALIDATING,
    SENDING_TRANSACTION,
    AWAITING_TRANSACTION_CONFIRMATION,
    COMPLETED,
    EXCEPTION;

    val asString: String
        get() = when (this) {
            NONE -> "none"
            VALIDATING -> "validating"
            SENDING_TRANSACTION -> "sendingTransaction"
            AWAITING_TRANSACTION_CONFIRMATION -> "awaitingTransactionConfirmation"
            COMPLETED -> "completed"
            EXCEPTION -> "error"
        }

    val description: String
        get() = when (this) {
            // Note: This should not be used.
            NONE -> "Press Close Swap to close the swap"
            VALIDATING -> "Checking that swap can be closed..."
            SENDING_TRANSACTION -> "Closing swap..."
            AWAITING_TRANSACTION_CONFIRMATION -> "Waiting for confirmation..."
            COMPLETED -> "Successfully closed swap."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }

}