package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.i18n.I18n
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferTruthSource
import com.commuto.interfacedesktop.offer.PreviewableOfferTruthSource

/**
 * Displays the [OffersListComposable] for open offers and the focused [Offer], if any.
 *
 * @param offerTruthSource An object implementing [OfferTruthSource] that acts as a single source of truth for all
 * offer-related data.
 */
@Composable
fun OffersComposable(offerTruthSource: OfferTruthSource) {

    // The offer to show in the offer detail composable
    val focusedOffer = remember { mutableStateOf<Offer?>(null) }

    Row {
        OffersListComposable(Modifier.widthIn(200.dp, 400.dp), offerTruthSource, focusedOffer)
        if (focusedOffer.value != null) {
            Text(text = "id: " + focusedOffer.value!!.id.toString())
        } else {
            Text(I18n.get("NoOfferFocused"))
        }
    }
}

/**
 * Displays a preview of [OffersComposable].
 */
@Preview
@Composable
fun PreviewOffersComposable() {
    OffersComposable(PreviewableOfferTruthSource())
}