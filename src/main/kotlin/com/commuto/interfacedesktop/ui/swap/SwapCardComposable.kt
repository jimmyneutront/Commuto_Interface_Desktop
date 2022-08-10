package com.commuto.interfacedesktop.ui.swap

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Displays a card with basic information about an swap, to be shown in the main list of swaps.
 *
 * @param swapDirection The direction of the swap that this card represents, as a [String].
 * @param stablecoinCode The currency code of the swap's stablecoin.
 */
@Composable
fun SwapCardComposable(swapDirection: String, stablecoinCode: String) {
    Box {
        Row {
            Column {
                Text(
                    text = swapDirection,
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
 * Displays a preview of [SwapCardComposable].
 */
@Preview
@Composable
fun PreviewSwapCardComposable() {
    SwapCardComposable(
        swapDirection = "Buy",
        stablecoinCode = "DAI"
    )
}