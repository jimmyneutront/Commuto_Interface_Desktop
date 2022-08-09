package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.Offer

/**
 * Displays a card with basic information about an offer, to be shown in the main list of open
 * offers.
 *
 * @param offerDirection The direction of the offer that this card represents, as a [String].
 * @param stablecoinCode The currency code of the offer's stablecoin.
 */
@Composable
fun OfferCardComposable(offerDirection: String, stablecoinCode: String) {
    Box {
        Row {
            Column {
                Text(
                    text = offerDirection,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stablecoinCode,
                    style = MaterialTheme.typography.h5
                )
            }
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Displays a preview of [OfferCardComposable] with a sample [Offer].
 */
@Preview
@Composable
fun PreviewOfferCardComposable() {
    OfferCardComposable(
        offerDirection = "Buy",
        stablecoinCode = "DAI"
    )
}