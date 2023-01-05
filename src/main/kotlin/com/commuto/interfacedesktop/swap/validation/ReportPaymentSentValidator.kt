package com.commuto.interfacedesktop.swap.validation

import com.commuto.interfacedesktop.swap.ReportingPaymentSentState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapRole
import com.commuto.interfacedesktop.swap.SwapState

/**
 * Ensures that [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent)
 * can be called for [swap].
 *
 * This function ensures that:
 * - [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) is not
 * already being called for [swap].
 * - the user is the buyer for [swap].
 * - the next step in the swapping process for [swap] is the user calling
 * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent)
 *
 * @param swap The [Swap] to be validated.
 *
 * @throws [SwapDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateSwapForReportingPaymentSent(swap: Swap) {
    if (swap.reportingPaymentSentState.value != ReportingPaymentSentState.NONE &&
        swap.reportingPaymentSentState.value != ReportingPaymentSentState.VALIDATING &&
        swap.reportingPaymentSentState.value != ReportingPaymentSentState.EXCEPTION) {
        throw SwapDataValidationException("Payment sending is already being reported this swap.")
    }
    // Ensure that the user is the buyer for the swap
    if (!(swap.role == SwapRole.MAKER_AND_BUYER || swap.role == SwapRole.TAKER_AND_BUYER)) {
        throw SwapDataValidationException("Only the Buyer can report sending payment")
    }
    // Ensure that this swap is awaiting reporting of payment sent
    if (swap.state.value != SwapState.AWAITING_PAYMENT_SENT) {
        throw SwapDataValidationException("Payment sending cannot currently be reported for this swap.")
    }
}