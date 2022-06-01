package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.Offer

@Composable
fun OffersListComposable(modifier: Modifier, viewModel: OffersViewModel, focusedOffer: MutableState<Offer?>) {
    Column(modifier = modifier) {
        Text(
            text = "Offers",
            style = MaterialTheme.typography.h2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        OffersDividerComposable()
        LazyColumn {
            items(viewModel.offers.value) { offer ->
                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    contentPadding = PaddingValues(10.dp),
                    onClick = {
                        focusedOffer.value = offer
                    }
                ) {
                    OfferCardComposable(offer)
                }
                OffersDividerComposable()
            }
        }
    }
}

@Composable
private fun OffersDividerComposable() {
    Divider(
        modifier = Modifier.padding(horizontal = 10.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    )
}

@Preview
@Composable
fun PreviewOffersListComposable() {
    OffersListComposable(Modifier.widthIn(0.dp, 400.dp), OffersViewModel(), mutableStateOf(null))
}