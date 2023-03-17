package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently filling a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the swap filling process we are currently in.
 *
 * @property NONE Indicates that we are not currently filling the corresponding swap.
 * @property VALIDATING Indicates that we are currently checking if we can call
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the swap.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will call
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the corresponding swap.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will call
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the corresponding swap, and
 * we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have called
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the corresponding swap, and
 * that the transaction to do so has been confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while calling
 * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the corresponding swap.
 * @property asString Returns a [String] corresponding to a particular case of [FillingSwapState]. This is primarily
 * used for database storage.
 * @property description A human-readable string describing the current state.
 */
enum class FillingSwapState {
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
            NONE -> "Press Fill Swap to fill the Swap"
            VALIDATING -> "Checking that the Swap can be filled..."
            SENDING_TRANSACTION -> "Filling Swap..."
            AWAITING_TRANSACTION_CONFIRMATION -> "Waiting for confirmation"
            COMPLETED -> "Successfully filled swap."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}