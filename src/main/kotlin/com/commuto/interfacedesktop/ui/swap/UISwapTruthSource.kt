package com.commuto.interfacedesktop.ui.swap

import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapTruthSource

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
    fun reportPaymentSent(swap: Swap)

    /**
     * Attempts to report that a seller has received fiat payment for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). This may only be used by the seller
     * in [swap].
     *
     * @param swap The [Swap] for which to report receiving payment.
     */
    fun reportPaymentReceived(swap: Swap)
}