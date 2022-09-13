package com.commuto.interfacedesktop.swap

/**
 * Indicates whether we are currently reporting that payment has been received for a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If we are, then this indicates the part
 * of the payment-received reporting process we are currently in.
 *
 * @property NONE Indicates that we are not currently reporting that payment is received for the corresponding swap.
 * @property CHECKING Indicates that we are currently checking if we can report that payment is received for the swap.
 * @property REPORTING Indicates that we are currently calling CommutoSwap's
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
 * function for the swap.
 * @property COMPLETED Indicates that we have reported that payment is received for the corresponding swap.
 * @property EXCEPTION Indicates that we encountered an exception while reporting that payment is received for the
 * corresponding swap.
 */
enum class ReportingPaymentReceivedState {
    NONE,
    CHECKING,
    REPORTING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when (this) {
            // Note: This should not be used.
            NONE -> "Press Report Payment Received to report that you have received payment"
            CHECKING -> "Checking that receiving payment can be reported..."
            REPORTING -> "Reporting that payment is received..."
            COMPLETED -> "Successfully reported that payment is received."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}