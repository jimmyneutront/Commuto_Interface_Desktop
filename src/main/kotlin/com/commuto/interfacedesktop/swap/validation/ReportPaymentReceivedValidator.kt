package com.commuto.interfacedesktop.swap.validation

import com.commuto.interfacedesktop.swap.ReportingPaymentReceivedState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapRole
import com.commuto.interfacedesktop.swap.SwapState

/**
 * Ensures that
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) can be
 * called for [swap].
 *
 * This function ensures that:
 * - [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received) is
 * not already being called for [swap].
 * - the user is the seller for [swap].
 * - the next step in the swapping process for [swap] is the user calling
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received).
 *
 * @param swap The [Swap] to be validated.
 *
 * @throws [SwapDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateSwapForReportingPaymentReceived(swap: Swap) {
    if (swap.reportingPaymentReceivedState.value != ReportingPaymentReceivedState.NONE &&
        swap.reportingPaymentReceivedState.value != ReportingPaymentReceivedState.VALIDATING &&
        swap.reportingPaymentReceivedState.value != ReportingPaymentReceivedState.EXCEPTION) {
        throw SwapDataValidationException("Payment receiving is already being reported this swap.")
    }
    if (!(swap.role == SwapRole.MAKER_AND_SELLER || swap.role == SwapRole.TAKER_AND_SELLER)) {
        throw SwapDataValidationException("Only the Seller can report receiving payment")
    }
    if (swap.state.value != SwapState.AWAITING_PAYMENT_RECEIVED) {
        throw SwapDataValidationException("Payment receiving cannot currently be reported for this swap.")
    }
}