package com.commuto.interfacedesktop.ui.offer

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
import com.commuto.interfacedesktop.ui.settlement.PreviewableSettlementMethodTruthSource
import com.commuto.interfacedesktop.ui.settlement.UISettlementMethodTruthSource
import java.math.BigInteger

/**
 * Displays the [OffersListComposable] for open offers and the focused [Offer], if any.
 *
 * @param offerTruthSource An object implementing [UIOfferTruthSource] that acts as a single source of truth for all
 * offer-related data.
 * @param settlementMethodTruthSource An object implementing [UISettlementMethodTruthSource] that acts as a single
 * source of truth for all settlement-method-related data.
 */
@Composable
fun OffersComposable(
    offerTruthSource: UIOfferTruthSource,
    settlementMethodTruthSource: UISettlementMethodTruthSource,
) {

    /**
     * Indicates which composable should be shown on the trailing side of [OffersListComposable]
     */
    val focusedOfferComposable = remember { mutableStateOf(FocusedOfferComposable.OfferComposable) }

    /**
     * The offer to show in the offer detail composable.
     */
    val focusedOffer = remember { mutableStateOf<Offer?>(null) }

    Row {
        OffersListComposable(
            modifier = Modifier.widthIn(100.dp, 300.dp),
            offerTruthSource = offerTruthSource,
            focusedOfferComposable = focusedOfferComposable,
            focusedOffer = focusedOffer,
        )
        when (focusedOfferComposable.value) {
            FocusedOfferComposable.OfferComposable -> {
                if (focusedOffer.value != null) {
                    OfferComposable(
                        offerTruthSource = offerTruthSource,
                        id = focusedOffer.value?.id,
                        settlementMethodTruthSource = settlementMethodTruthSource,
                        focusedOfferComposable = focusedOfferComposable,
                    )
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
            // TODO: Get actual chain ID here
            FocusedOfferComposable.OpenOfferComposable -> {
                OpenOfferComposable(
                    offerTruthSource = offerTruthSource,
                    settlementMethodTruthSource = settlementMethodTruthSource,
                    chainID = BigInteger.valueOf(31337L)
                )
            }
            FocusedOfferComposable.EditOfferComposable -> {
                EditOfferComposable(
                    offer = focusedOffer.value,
                    offerTruthSource = offerTruthSource,
                    settlementMethodTruthSource = settlementMethodTruthSource,
                    stablecoinCurrencyCode = "STBL",
                    focusedOfferComposable = focusedOfferComposable
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
    OffersComposable(
        PreviewableOfferTruthSource(),
        PreviewableSettlementMethodTruthSource(),
    )
}