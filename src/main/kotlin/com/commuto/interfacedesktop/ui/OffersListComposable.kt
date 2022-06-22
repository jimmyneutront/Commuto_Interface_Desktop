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
import com.commuto.interfacedesktop.i18n.I18n
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService

/**
 * Displays a [OffersNoneFoundComposable] if there are no open offers in [viewModel], or, if there
 * are open offers in [viewModel], displays a list containing an [OfferCardComposable]-labeled
 * [Button] for each open offer in [viewModel] that navigates to "OfferDetailComposable/ + offer id
 * as a [String]" when pressed.
 *
 * @param viewModel The OffersViewModel that acts as a single source of truth for all offer-related
 * data.
 * @param focusedOffer The currently focused [Offer], the information of which will be displayed next to the list of
 * open [Offer]s.
 */
@Composable
fun OffersListComposable(modifier: Modifier, viewModel: OffersViewModel, focusedOffer: MutableState<Offer?>) {
    Column(modifier = modifier) {
        Text(
            text = I18n.get("Offers"),
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

/**
 * Displays a horizontal divider.
 */
@Composable
private fun OffersDividerComposable() {
    Divider(
        modifier = Modifier.padding(horizontal = 10.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    )
}

/**
 * Displays the localized, vertically and horizontally centered words "No Offers Found".
 */
@Composable
private fun OffersNoneFoundComposable() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = I18n.get("NoOffersFound"),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/**
 * Displays a preview of [OffersListComposable].
 */
@Preview
@Composable
fun PreviewOffersListComposable() {
    OffersListComposable(Modifier.widthIn(0.dp, 400.dp), OffersViewModel(OfferService()), mutableStateOf(null))
}