package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.i18n.I18n
import com.commuto.interfacedesktop.offer.Offer
import java.math.BigInteger

/**
 * Displays the [OffersListComposable] for open offers and the focused [Offer], if any.
 *
 * @param offerTruthSource An object implementing [UIOfferTruthSource] that acts as a single source of truth for all
 * offer-related data.
 */
@Composable
fun OffersComposable(offerTruthSource: UIOfferTruthSource) {

    /**
     * Indicates which composable should be shown on the trailing side of [OffersListComposable]
     */
    val focusedOfferComposable = remember { mutableStateOf(FocusedOfferComposable.OfferComposable) }

    /**
     * The offer to show in the offer detail composable.
     */
    val focusedOffer = remember { mutableStateOf<Offer?>(null) }

    Row {
        OffersListComposable(Modifier.widthIn(100.dp, 300.dp), offerTruthSource, focusedOfferComposable, focusedOffer)
        when (focusedOfferComposable.value) {
            FocusedOfferComposable.OfferComposable -> {
                if (focusedOffer.value != null) {
                    OfferComposable(offerTruthSource, focusedOffer.value!!.id)
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(I18n.get("NoOfferFocused"))
                    }
                }
            }
            FocusedOfferComposable.CreateOfferComposable -> {
                CreateOfferComposable(
                    offerTruthSource = offerTruthSource,
                    chainID = BigInteger.ONE
                )
            }
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