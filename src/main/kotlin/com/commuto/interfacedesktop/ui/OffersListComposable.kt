package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService

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
        if (viewModel.offerService.offers.size == 0) {
            OffersNoneFoundComposable()
        } else {
            LazyColumn {
                items(viewModel.offerService.offers) { offer ->
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
}

@Composable
private fun OffersDividerComposable() {
    Divider(
        modifier = Modifier.padding(horizontal = 10.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    )
}

@Composable
private fun OffersNoneFoundComposable() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "No offers found",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Preview
@Composable
fun PreviewOffersListComposable() {
    OffersListComposable(Modifier.widthIn(0.dp, 400.dp), OffersViewModel(OfferService()), mutableStateOf(null))
}