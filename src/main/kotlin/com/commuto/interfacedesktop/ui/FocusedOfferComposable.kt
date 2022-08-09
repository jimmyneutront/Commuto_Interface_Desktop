package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.Composable

/**
 * Indicates the current [Composable] that should be focused in [OffersComposable], appearing on the trailing side of
 * the [OffersListComposable]
 */
enum class FocusedOfferComposable {
    OfferComposable, OpenOfferComposable, EditOfferComposable,
}