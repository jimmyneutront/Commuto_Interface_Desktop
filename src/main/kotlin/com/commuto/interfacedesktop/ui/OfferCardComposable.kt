package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.Offer

@Composable
fun OfferCardComposable(offer: Offer) {
    Box {
        Row {
            Column {
                Text(
                    text = offer.direction,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = offer.price + " " + offer.pair,
                    style = MaterialTheme.typography.h5
                )
            }
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
fun PreviewOfferCardComposable() {
    OfferCardComposable(
        offer = Offer.sampleOffers[0],
    )
}