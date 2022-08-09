package com.commuto.interfacedesktop.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.SettlementMethod
import java.lang.NumberFormatException
import java.math.BigDecimal

/**
 * Displays a vertical list of settlement method cards, one for each settlement method that the user has created. These
 * cards can be tapped to indicate that the user is willing to use them to send/receive payment for the offer being
 * opened.
 *
 * @param settlementMethods A [List] of [SettlementMethod]s to be displayed.
 * @param stablecoinCurrencyCode The currency code of the currently selected stablecoin.
 * @param selectedSettlementMethods A [SnapshotStateList] of [SettlementMethod]s that the user has selected.
 */
@Composable
fun SettlementMethodSelector(
    settlementMethods: List<SettlementMethod>,
    stablecoinCurrencyCode: String,
    selectedSettlementMethods: SnapshotStateList<SettlementMethod>
) {
    Column {
        for (settlementMethod in settlementMethods) {
            SettlementMethodCardComposable(
                settlementMethod = settlementMethod,
                stablecoinCurrencyCode = stablecoinCurrencyCode,
                selectedSettlementMethods = selectedSettlementMethods
            )
            Spacer(
                modifier = Modifier.height(5.dp)
            )
        }
    }
}

/**
 * Displays information about a particular settlement method. If the settlement method that this represents is present
 * in [selectedSettlementMethods], the outline and currency description become green. The user can tap on the price
 * description to expose a text field in which they can specify a positive price for this settlement method with up to
 * six decimal places of precision.
 *
 * @param settlementMethod The [SettlementMethod] that this represents.
 * @param stablecoinCurrencyCode The currency code of the stablecoin selected by the user.
 * @param selectedSettlementMethods A [SnapshotStateList] of [SettlementMethod]s that the user has selected.
 */
@Composable
fun SettlementMethodCardComposable(
    settlementMethod: SettlementMethod,
    stablecoinCurrencyCode: String,
    selectedSettlementMethods: SnapshotStateList<SettlementMethod>,
) {

    val isSelected = remember { mutableStateOf(false) }

    val color = if (isSelected.value) Color.Green else Color.Black

    val isEditingPrice = remember { mutableStateOf(false) }

    val priceString = remember { mutableStateOf(settlementMethod.price) }

    val price = remember { mutableStateOf(BigDecimal.ZERO) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = color, RoundedCornerShape(10.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(PaddingValues(horizontal = 10.dp))
        ) {
            Text(
                text = buildCurrencyDescription(settlementMethod),
                color = color,
                modifier = Modifier.padding(PaddingValues(top = 10.dp))
            )
            Button(
                onClick = {
                    isEditingPrice.value = !isEditingPrice.value
                },
                content = {
                    Text(
                        text = buildOpenOfferPriceDescription(settlementMethod, stablecoinCurrencyCode)
                    )
                },
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                elevation = null
            )
            if (isEditingPrice.value) {
                TextField(
                    label = { Text("Price (${stablecoinCurrencyCode}/${settlementMethod.currency})") },
                    value = priceString.value,
                    onValueChange = {
                        if (it == "") {
                            price.value = BigDecimal.ZERO
                            settlementMethod.price = ""
                        } else {
                            try {
                                val newPrice = BigDecimal(it)
                                // Don't allow negative values, and limit to 6 decimal places
                                if (newPrice >= BigDecimal.ZERO && newPrice.scale() <= 6) {
                                    price.value = newPrice
                                    settlementMethod.price = it
                                    priceString.value = it
                                }
                            } catch (exception: NumberFormatException) {
                                /*
                                If the change prevents us from creating a BigDecimal minimum amount value, then we
                                don't allow that change
                                 */
                            }
                        }
                    }
                )
            }
        }
        Switch(
            checked = isSelected.value,
            onCheckedChange = { newCheckedValue ->
                if (!newCheckedValue) {
                    selectedSettlementMethods.removeIf {
                        it.method == settlementMethod.method && it.currency == settlementMethod.currency
                    }
                } else {
                    selectedSettlementMethods.add(settlementMethod)
                }
                isSelected.value = newCheckedValue
            },
        )
    }
}

/**
 * Builds a human-readable string describing the price specified for a settlement method, such as "Price: 0.94 EUR/DAI"
 * or "Price: 1.00 USD/USDC", or "Price: Tap to specify" if no price has been specified.
 *
 * @param settlementMethod The [SettlementMethod] for which this price description is being built.
 * @param stablecoinCurrencyCode The currency code of the stablecoin that the user has selected.
 */
fun buildOpenOfferPriceDescription(
    settlementMethod: SettlementMethod,
    stablecoinCurrencyCode: String,
): String {
    return if (settlementMethod.price == "") {
        "Price: Tap to specify"
    } else {
        "Price: ${settlementMethod.price} ${settlementMethod.currency}/${stablecoinCurrencyCode}"
    }
}