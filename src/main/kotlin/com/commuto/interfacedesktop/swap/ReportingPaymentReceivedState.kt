package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently reporting that payment has been received for a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the payment-received reporting process we are currently in.
 *
 * @property NONE Indicates that we are not currently reporting that payment is received for the corresponding swap.
 * @property VALIDATING Indicates that we are currently checking if we can call
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) for
 * the swap.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will call
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) for
 * the corresponding swap.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will call
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) for
 * the corresponding swap, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have called
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) for
 * the corresponding swap, and that the transaction to do so has been confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while calling
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) for
 * the corresponding swap.
 * @property asString Returns a [String] corresponding to a particular case of [ReportingPaymentSentState]. This is
 * primarily used for database storage.
 * @property description A human readable string describing the current state.
 */
enum class ReportingPaymentReceivedState {
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
            NONE -> "Press Report Payment Received to report that you have received payment"
            VALIDATING -> "Checking that payment receiving can be reported..."
            SENDING_TRANSACTION -> "Reporting that payment is received..."
            AWAITING_TRANSACTION_CONFIRMATION -> "Waiting for confirmation..."
            COMPLETED -> "Successfully reported that payment is received."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}