package com.commuto.interfacedesktop.ui.offer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData
import com.commuto.interfacedesktop.ui.settlement.*
import java.lang.NumberFormatException
import java.math.BigDecimal

/**
 * Displays a vertical list of settlement method cards, one for each settlement method that the user has created. These
 * cards can be tapped to indicate that the user is willing to use them to send/receive payment for the offer being
 * opened.
 *
 * @param settlementMethodTruthSource An object implementing [UISettlementMethodTruthSource] that acts as a single
 * source of truth for all settlement-method-related data.
 * @param stablecoinCurrencyCode The currency code of the currently selected stablecoin.
 * @param selectedSettlementMethods A [SnapshotStateList] of [SettlementMethod]s that the user has selected.
 */
@Composable
fun SettlementMethodSelector(
    settlementMethodTruthSource: UISettlementMethodTruthSource,
    stablecoinCurrencyCode: String,
    selectedSettlementMethods: SnapshotStateList<SettlementMethod>
) {
    if (settlementMethodTruthSource.settlementMethods.size > 0) {
        Column {
            for (settlementMethod in settlementMethodTruthSource.settlementMethods) {
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
    } else {
        Text(
            text = "You haven't added any settlement methods."
        )
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

    /**
     * An object implementing [PrivateData] containing private data for [settlementMethod].
     */
    val privateData = remember { mutableStateOf<PrivateData?>(null) }
    /**
     * Indicates whether we have finished attempting to parse the private data associated with [settlementMethod].
     */
    val finishedParsingData = remember { mutableStateOf(false) }
    /**
     * Indicates whether the  user has selected the [SettlementMethod] that this  card represents.
     */
    val isSelected = remember { mutableStateOf(false) }
    /**
     * The color of the outline of this card. If this card is selected, it should be outlined in green. Otherwise, it
     * should be outlined in black.
     */
    val color = if (isSelected.value) Color.Green else Color.Black
    /**
     * Indicates whether the user is currently editing this [SettlementMethod]'s price.
     */
    val isEditingPrice = remember { mutableStateOf(false) }
    /**
     * The current price for this settlement method, as a string.
     */
    val priceString = remember { mutableStateOf(settlementMethod.price) }
    /**
     * The current price for this settlement method, as a [BigDecimal].
     */
    val price = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The actual [SettlementMethod] object that this composable represents. If there is no [SettlementMethod] in
     * [selectedSettlementMethods] with method and currency values equal to [settlementMethod], then this simply points
     * to [settlementMethod]. However, if such a [SettlementMethod] is found in [selectedSettlementMethods], then this
     * is updated to point to that [SettlementMethod] object.
     */
    var actualSettlementMethod = settlementMethod

    LaunchedEffect(true) {
        createPrivateDataObjectForUI(
            settlementMethod = settlementMethod,
            privateData = privateData,
            finishedParsingData = finishedParsingData
        )
    }

    selectedSettlementMethods.firstOrNull {
        it.method == settlementMethod.method && it.currency == settlementMethod.currency
    }?.let {
        /*
        If there is a settlement method in selectedSettlementMethods with its method and currency values matching those
        of the passed settlementMethod, then the settlement method that this card represents has been selected, and
        therefore isSelected should be true, the price and priceString values of this composable should be set using
        the price value of the SettlementMethod in selectedSettlementMethods, and then we set actualSettlementMethod
        equal to the SettlementMethod object in selectedSettlementMethods, so that price changes made by this Composable
        are made to the SettlementMethod in selectedSettlementMethods rather than the to the settlementMethod parameter,
        which would cause price changes to be discarded once this Composable disappears.
         */
        actualSettlementMethod = it
        if (actualSettlementMethod.price != "") {
            try {
                val newPrice = BigDecimal(actualSettlementMethod.price)
                // Don't allow negative values, and limit to 6 decimal places
                if (newPrice >= BigDecimal.ZERO && newPrice.scale() <= 6) {
                    price.value = newPrice
                    priceString.value = actualSettlementMethod.price
                } else {
                    // If we do somehow have a negative value or more than 6 decimal places, we set the price to zero.
                    actualSettlementMethod.price = BigDecimal.ZERO.toString()
                }
            } catch (exception: NumberFormatException) {
                /*
                If the settlement method's price has somehow become corrupted in a way that prevents us from creating a
                BigDecimal price value, then we set the price to zero.
                 */
                actualSettlementMethod.price = BigDecimal.ZERO.toString()
            }
        }
        isSelected.value = true
    }

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
                text = buildCurrencyDescription(actualSettlementMethod),
                color = color,
                modifier = Modifier.padding(PaddingValues(top = 10.dp))
            )
            Button(
                onClick = {
                    isEditingPrice.value = !isEditingPrice.value
                },
                content = {
                    Text(
                        text = buildOpenOfferPriceDescription(actualSettlementMethod, stablecoinCurrencyCode)
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
                    label = { Text("Price (${stablecoinCurrencyCode}/${actualSettlementMethod.currency})") },
                    value = priceString.value,
                    onValueChange = {
                        if (it == "") {
                            price.value = BigDecimal.ZERO
                            actualSettlementMethod.price = ""
                        } else {
                            try {
                                val newPrice = BigDecimal(it)
                                // Don't allow negative values, and limit to 6 decimal places
                                if (newPrice >= BigDecimal.ZERO && newPrice.scale() <= 6) {
                                    price.value = newPrice
                                    actualSettlementMethod.price = it
                                    priceString.value = it
                                }
                            } catch (exception: NumberFormatException) {
                                /*
                                 If the change prevents us from creating a BigDecimal price value, then we don't allow
                                 that change
                                 */
                            }
                        }
                    }
                )
            }
            SettlementMethodPrivateDetailComposable(settlementMethod = settlementMethod)
        }
        Switch(
            checked = isSelected.value,
            onCheckedChange = { newCheckedValue ->
                if (!newCheckedValue) {
                    selectedSettlementMethods.removeIf {
                        it.method == actualSettlementMethod.method && it.currency == actualSettlementMethod.currency
                    }
                } else {
                    if (selectedSettlementMethods.firstOrNull {
                            it.method == actualSettlementMethod.method && it.currency == actualSettlementMethod.currency
                        } == null) {
                        /*
                        We should only add the settlement method that this card represents if it is not currently in
                        selectedSettlementMethods
                         */
                        selectedSettlementMethods.add(actualSettlementMethod)
                    }
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