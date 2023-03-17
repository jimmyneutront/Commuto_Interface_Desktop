package com.commuto.interfacedesktop.dispute

/**
 * Indicates whether we are currently raising a dispute for a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the dispute raising process we are currently in.
 *
 * @property NONE Indicates that we are not currently raising a dispute for the corresponding swap.
 * @property VALIDATING Indicates that we are currently checking if we can call
 * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) for the swap.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will call
 * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) for the corresponding
 * swap.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will call
 * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) for the corresponding
 * swap, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have called
 * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) for the corresponding
 * swap, and that the transaction to do so has been confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while calling
 * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) for the corresponding
 * swap.
 * @property asString Returns a [String] corresponding to a particular case of [RaisingDisputeState]. This is primarily
 * used for database storage.
 */
enum class RaisingDisputeState {
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

}