package com.commuto.interfacedesktop.ui.swap

import androidx.compose.runtime.mutableStateMapOf
import com.commuto.interfacedesktop.swap.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.web3j.crypto.RawTransaction
import java.util.*
import javax.inject.Inject

/**
 * The Swap View Model, the single source of truth for all swap-related data. It is observed by swap-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @param swapService The [SwapService] used for completing the swap process for [Swap]s in [swaps].
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property swaps A mutable state map of [UUID]s to [Swap]s that acts as a single source of truth for all
 * offer-related data.
 */
class SwapViewModel @Inject constructor(private val swapService: SwapService): UISwapTruthSource {

    private val logger = LoggerFactory.getLogger(javaClass)

    override var swaps = mutableStateMapOf<UUID, Swap>().also { map ->
        Swap.sampleSwaps.map {
            map[it.id] = it
        }
    }

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    /**
     * Adds a new [Swap] to [swaps].
     *
     * @param swap The new [Swap] to be added to [swaps].
     */
    override fun addSwap(swap: Swap) {
        swaps[swap.id] = swap
    }

    /**
     * Sets the value of [Swap.fillingSwapState] of [swap] to [state] on the main coroutine dispatcher.
     *
     * @param swap The [Swap] of which to set the [Swap.fillingSwapState] value.
     * @param state The value to which [swap]'s [Swap.fillingSwapState] value will be set.
     */
    private suspend fun setFillingSwapState(swap: Swap, state: FillingSwapState) {
        withContext(Dispatchers.Main) {
            swap.fillingSwapState.value = state
        }
    }

    /**
     * Sets the value of [Swap.reportingPaymentSentState] of [swap] to [state] on the main coroutine dispatcher.
     *
     * @param swap The [Swap] of which to set the [Swap.reportingPaymentSentState] value.
     * @param state The value to which [swap]'s [Swap.reportingPaymentSentState] value will be set.
     */
    private suspend fun setReportingPaymentSentState(swap: Swap, state: ReportingPaymentSentState) {
        withContext(Dispatchers.Main) {
            swap.reportingPaymentSentState.value = state
        }
    }

    /**
     * Sets the value of [Swap.reportingPaymentReceivedState] of [swap] to [state] on the main coroutine dispatcher.
     *
     * @param swap The [Swap] of which to set the [Swap.reportingPaymentReceivedState] value.
     * @param state The value to which [swap]'s [Swap.reportingPaymentReceivedState] value will be set.
     */
    private suspend fun setReportingPaymentReceivedState(swap: Swap, state: ReportingPaymentReceivedState) {
        withContext(Dispatchers.Main) {
            swap.reportingPaymentReceivedState.value = state
        }
    }

    /**
     * Sets the value of [Swap.closingSwapState] of [swap] to [state] on the main coroutine dispatcher.
     *
     * @param swap The [Swap] of which to set the [Swap.closingSwapState] value.
     * @param state The value to which [swap]'s [Swap.closingSwapState] value will be set.
     */
    private suspend fun setClosingSwapState(swap: Swap, state: ClosingSwapState) {
        withContext(Dispatchers.Main) {
            swap.closingSwapState.value = state
        }
    }

    /**
     * Attempts to fill a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] to fill.
     */
    override fun fillSwap(swap: Swap) {
        viewModelScope.launch {
            try {
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.CHECKING
                )
                logger.info("fillSwap: filling ${swap.id}")
                swapService.fillSwap(
                    swapToFill = swap,
                    afterPossibilityCheck = {
                        setFillingSwapState(
                            swap = swap,
                            state = FillingSwapState.APPROVING
                        )
                    },
                    afterTransferApproval = {
                        setFillingSwapState(
                            swap = swap,
                            state = FillingSwapState.FILLING
                        )
                    }
                )
                logger.info("fillSwap: successfully filled ${swap.id}")
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.error("fillSwap: got exception during fillSwap call for ${swap.id}", exception)
                swap.fillingSwapException = exception
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.EXCEPTION
                )
            }
        }
    }

    /**
     * Attempts to report that a buyer has sent fiat payment for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). This may only be used by the buyer in
     * [swap].
     *
     * @param swap The [Swap] for which to report sending payment.
     */
    @Deprecated("Use the new transaction pipeline with improved transaction state management")
    override fun reportPaymentSent(swap: Swap) {
        /*
        viewModelScope.launch {
            setReportingPaymentSentState(
                swap = swap,
                state = ReportingPaymentSentState.CHECKING
            )
            try {
                logger.info("reportPaymentSent: reporting for ${swap.id}")
                swapService.reportPaymentSent(
                    swap = swap,
                    afterPossibilityCheck = {
                        setReportingPaymentSentState(
                            swap = swap,
                            state = ReportingPaymentSentState.REPORTING,
                        )
                    },
                )
                logger.info("reportPaymentSent: successfully reported for ${swap.id}")
                setReportingPaymentSentState(
                    swap = swap,
                    state = ReportingPaymentSentState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.error("reportPaymentSent: got exception during reportPaymentSent call for ${swap.id}", exception)
                swap.reportingPaymentSentException = exception
                setReportingPaymentSentState(
                    swap = swap,
                    state = ReportingPaymentSentState.EXCEPTION
                )
            }
        }*/
    }

    /**
     * Attempts to create a [RawTransaction] to call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for
     * [swap], for which the user of this interface should be the buyer.
     *
     * This passes [swap]'s ID and chain ID to [SwapService.createReportPaymentSentTransaction] and then passes the
     * resulting transaction to [createdTransactionHandler] or [Exception] to [exceptionHandler].
     *
     * @param swap The [Swap] for which
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) will be
     * called.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createReportPaymentSentTransaction(
        swap: Swap,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            logger.info("createReportPaymentSentTransaction: creating for ${swap.id}")
            try {
                val createdTransaction = swapService.createReportPaymentSentTransaction(
                    swapID = swap.id,
                    chainID = swap.chainID
                )
                createdTransactionHandler(createdTransaction)
            } catch (exception: Exception) {
                exceptionHandler(exception)
            }
        }
    }

    /**
     * Attempts to call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which the user of this interface
     * is the buyer.
     *
     * This clears the reporting-payment-sent-related [Exception] of [swap] and sets [swap]'s
     * [Swap.reportingPaymentSentState] to [ReportingPaymentSentState.VALIDATING], and then passes all data to
     * [swapService]'s [SwapService.reportPaymentSent] function.
     *
     * @param swap The [Swap] for which
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) will be
     * called.
     * @param reportPaymentSentTransaction An optional [RawTransaction] that will call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for
     * [swap].
     */
    override fun reportPaymentSent(swap: Swap, reportPaymentSentTransaction: RawTransaction?) {
        swap.reportingPaymentSentException = null
        swap.reportingPaymentSentState.value = ReportingPaymentSentState.VALIDATING
        viewModelScope.launch {
            logger.info("reportPaymentSent: reporting for ${swap.id}")
            try {
                swapService.reportPaymentSent(
                    swap = swap,
                    reportPaymentSentTransaction = reportPaymentSentTransaction,
                )
                logger.info("reportPaymentSent: successfully broadcast transaction for ${swap.id}")
            } catch (exception: Exception) {
                logger.error("reportPaymentSent: got exception during reportPaymentSent call for ${swap.id}", exception)
                withContext(Dispatchers.Main) {
                    swap.reportingPaymentSentException = exception
                    swap.reportingPaymentSentState.value = ReportingPaymentSentState.EXCEPTION
                }
            }
        }
    }

    /**
     * Attempts to report that a seller has received fiat payment for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). This may only be used by the seller
     * in [swap].
     *
     * @param swap The [Swap] for which to report receiving payment.
     */
    override fun reportPaymentReceived(swap: Swap) {
        viewModelScope.launch {
            setReportingPaymentReceivedState(
                swap = swap,
                state = ReportingPaymentReceivedState.CHECKING
            )
            try {
                logger.info("reportPaymentReceived: reporting for ${swap.id}")
                swapService.reportPaymentReceived(
                    swap = swap,
                    afterPossibilityCheck = {
                        setReportingPaymentReceivedState(
                            swap = swap,
                            state = ReportingPaymentReceivedState.REPORTING,
                        )
                    },
                )
                logger.info("reportPaymentReceived: successfully reported for ${swap.id}")
                setReportingPaymentReceivedState(
                    swap = swap,
                    state = ReportingPaymentReceivedState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.error("reportPaymentReceived: got exception during reportPaymentReceived call for ${swap.id}",
                    exception)
                swap.reportingPaymentReceivedException = exception
                setReportingPaymentReceivedState(
                    swap = swap,
                    state = ReportingPaymentReceivedState.EXCEPTION
                )
            }
        }
    }

    /**
     * Attempts to close a swap [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] to close.
     */
    override fun closeSwap(swap: Swap) {
        viewModelScope.launch {
            setClosingSwapState(
                swap = swap,
                state = ClosingSwapState.CHECKING,
            )
            try {
                logger.info("closeSwap: closing ${swap.id}")
                swapService.closeSwap(
                    swap = swap,
                    afterPossibilityCheck = {
                        setClosingSwapState(
                            swap = swap,
                            state = ClosingSwapState.CLOSING,
                        )
                    }
                )
                logger.info("closeSwap: successfully closed ${swap.id}")
                setClosingSwapState(
                    swap = swap,
                    state = ClosingSwapState.CHECKING,
                )
            } catch (exception: Exception) {
                logger.error("closeSwap: got exception during closeSwap call for ${swap.id}", exception)
                swap.closingSwapException = exception
                setClosingSwapState(
                    swap = swap,
                    state = ClosingSwapState.EXCEPTION
                )
            }
        }
    }

}