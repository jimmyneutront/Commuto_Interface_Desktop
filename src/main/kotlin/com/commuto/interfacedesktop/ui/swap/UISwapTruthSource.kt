package com.commuto.interfacedesktop.ui.swap

import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapTruthSource
import org.web3j.crypto.RawTransaction

/**
 * An interface that a class must adopt in order to act as a single source of truth for swap-related data in an
 * application with a graphical user interface.
 */
interface UISwapTruthSource: SwapTruthSource {
    /**
     * Attempts to fill a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] to fill.
     */
    fun fillSwap(
        swap: Swap
    )

    /**
     * Attempts to report that a buyer has sent fiat payment for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). This may only be used by the buyer in
     * [swap].
     *
     * @param swap The [Swap] for which to report sending payment.
     */
    @Deprecated("Use the new transaction pipeline with improved transaction state management")
    fun reportPaymentSent(swap: Swap)

    /**
     * Attempts to create a [RawTransaction] to call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for
     * [swap], for which the user of this interface should be the buyer.
     *
     * @param swap The [Swap] for which
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) will be
     * called.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createReportPaymentSentTransaction(
        swap: Swap,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    )

    /**
     * Attempts to call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which the user of this interface
     * is the buyer.
     *
     * @param swap The [Swap] for which
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) will be
     * called.
     * @param reportPaymentSentTransaction An optional [RawTransaction] that will call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for
     * [swap].
     */
    fun reportPaymentSent(swap: Swap, reportPaymentSentTransaction: RawTransaction?)

    /**
     * Attempts to report that a seller has received fiat payment for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). This may only be used by the seller
     * in [swap].
     *
     * @param swap The [Swap] for which to report receiving payment.
     */
    @Deprecated("Use the new transaction pipeline with improved transaction state management")
    fun reportPaymentReceived(swap: Swap)

    /**
     * Attempts to create a [RawTransaction] to call
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * for [swap], for which the user of this interface should be the seller.
     *
     * @param swap The [Swap] for which
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * will be called.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createReportPaymentReceivedTransaction(
        swap: Swap,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    )

    /**
     * Attempts to call
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * for a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which the user of this
     * interface is the buyer.
     *
     * @param swap The [Swap] for which
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * will be called.
     * @param reportPaymentReceivedTransaction An optional [RawTransaction] that will call
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * for [swap].
     */
    fun reportPaymentReceived(swap: Swap, reportPaymentReceivedTransaction: RawTransaction?)

    /**
     * Attempts to close a swap [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] to close.
     */
    @Deprecated("Use the new transaction pipeline with improved transaction state management")
    fun closeSwap(swap: Swap)

    /**
     * Attempts to create a [RawTransaction] to call
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for [swap].
     *
     * @param swap The [Swap] for which
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) will be called.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createCloseSwapTransaction(
        swap: Swap,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    )

    /**
     * Attempts to call
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap)
     * for a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] for which
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) will be called.
     * @param closeSwapTransaction An optional [RawTransaction] that will call
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for [swap].
     */
    fun closeSwap(swap: Swap, closeSwapTransaction: RawTransaction?)
}