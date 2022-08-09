package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.EditingOfferState
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.SettlementMethod

/**
 * Allows the user to edit one of their [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)s.
 *
 * @param offer The [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) for which this view
 * allows editing.
 * @param offerTruthSource The [OffersViewModel] that acts as a single source of truth for all offer-related data.
 * @param stablecoinCurrencyCode The currency code of the offer's stablecoin.
 */
@Composable
fun EditOfferComposable(
    offer: Offer?,
    offerTruthSource: UIOfferTruthSource,
    stablecoinCurrencyCode: String,
    focusedOfferComposable: MutableState<FocusedOfferComposable>
) {

    /*
     We want a copy of the list of sample settlement methods, we don't want to actually change any of them at all.
      */
    val settlementMethods = remember {
        SettlementMethod.sampleSettlementMethodsEmptyPrices.map {
            it.copy()
        }
    }

    if (offer == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Offer is not available.",
            )
        }
    } else {
        DisposableEffect(null) {
            onDispose {
                // Clear the "Offer Edited" message once the user leaves EditOfferView
                if (offer.editingOfferState.value == EditingOfferState.COMPLETED) {
                    offer.editingOfferState.value = EditingOfferState.NONE
                }
            }
        }
        val editOfferButtonLabel = if (offer.editingOfferState.value != EditingOfferState.EDITING) "Edit Offer" else
            "Editing Offer"
        val editOfferButtonOutlineColor = if (offer.editingOfferState.value != EditingOfferState.EDITING) Color.Black
            else Color.Gray

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(9.dp)
        ) {
            Button(
                onClick = {
                    focusedOfferComposable.value = FocusedOfferComposable.OfferComposable
                },
                content = {
                    Text(
                        text = "Back",
                        fontWeight = FontWeight.Bold,
                    )
                },
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                elevation = null,
                modifier = Modifier.padding(PaddingValues(top = 4.dp))
            )
            Text(
                text = "Edit Offer",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Settlement Methods:",
                style =  MaterialTheme.typography.h6,
            )
            SettlementMethodSelector(
                settlementMethods = settlementMethods,
                stablecoinCurrencyCode = stablecoinCurrencyCode,
                selectedSettlementMethods = offer.selectedSettlementMethods
            )
            if (offer.editingOfferState.value == EditingOfferState.EXCEPTION) {
                Text(
                    text = offer.editingOfferException?.message ?: "An unknown exception occurred",
                    style =  MaterialTheme.typography.h6,
                    color = Color.Red
                )
            } else if (offer.editingOfferState.value == EditingOfferState.COMPLETED) {
                Text(
                    text = "Offer Edited.",
                    style =  MaterialTheme.typography.h6,
                )
            }
            Button(
                onClick = {
                    // Don't let the user try to edit the offer if it is currently being edited
                    if (offer.editingOfferState.value != EditingOfferState.EDITING) {
                        offerTruthSource.editOffer(
                            offer = offer,
                            newSettlementMethods = offer.selectedSettlementMethods
                        )
                    }
                },
                content = {
                    Text(
                        text = editOfferButtonLabel,
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                border = BorderStroke(3.dp, editOfferButtonOutlineColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                elevation = null,
                modifier = Modifier.width(400.dp),
            )
        }
    }

}

/**
 * Displays a preview of [EditOfferComposable] with the stablecoin currency code "STBL".
 */
@Preview
@Composable
fun PreviewEditOfferComposable() {
    EditOfferComposable(
        offer = Offer.sampleOffers[0],
        offerTruthSource = PreviewableOfferTruthSource(),
        stablecoinCurrencyCode = "STBL",
        focusedOfferComposable = mutableStateOf(FocusedOfferComposable.EditOfferComposable),
    )
}