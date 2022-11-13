package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.ui.offer.OffersComposable
import com.commuto.interfacedesktop.ui.settlement.SettlementMethodsComposable
import com.commuto.interfacedesktop.ui.swap.SwapsComposable

/**
 * Indicates which tab view should be displayed to the user.
 *
 * @property OFFERS Indicates that [OffersComposable] should be displayed to the user.
 * @property SWAPS Indicates that [SwapsComposable] should be displayed to the user.
 * @property SETTLEMENT_METHODS Indicates that [SettlementMethodsComposable] should be displayed to the user.
 */
enum class CurrentTab {
    OFFERS,
    SWAPS,
    SETTLEMENT_METHODS;
}
