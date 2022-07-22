package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OfferTruthSource
import com.commuto.interfacedesktop.offer.PreviewableOfferTruthSource
import com.commuto.interfacedesktop.offer.SettlementMethod
import java.lang.NumberFormatException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * The screen for creating a new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param offerTruthSource The OffersViewModel that acts as a single source of truth for all offer-related data.
 * @param chainID The ID of the blockchain on which the new offer will be created.
 * @param stablecoins A [StablecoinInformationRepository] for all supported stablecoins on the blockchain specified by
 * [chainID].
 */
@Composable
fun CreateOfferComposable(
    offerTruthSource: OfferTruthSource,
    chainID: BigInteger,
    stablecoins: StablecoinInformationRepository = StablecoinInformationRepository.ethereumMainnetStablecoinInfoRepo
) {

    /**
     * The direction of the new offer, or null if the user has not specified a direction.
     */
    val direction = remember { mutableStateOf<OfferDirection?>(null) }

    /**
     * The contract address of the stablecoin that the user is offering to swap, or null if the user has not specified
     * a stablecoin.
     */
    val selectedStablecoin = remember { mutableStateOf<String?>(null) }

    /**
     * The currency code of the specified stablecoin, or "Stablecoin" if the user has not specified a stablecoin.
     */
    val stablecoinCurrencyCode = stablecoins
        .getStablecoinInformation(chainID, selectedStablecoin.value ?: "")?.currencyCode ?: "Stablecoin"

    /**
     * The specified minimum stablecoin amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val minimumAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified minimum stablecoin amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val minimumAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The specified maximum stablecoin amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val maximumAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified maximum stablecoin amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val maximumAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The specified security deposit amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val securityDepositAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified security deposit amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val securityDepositAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The list of [SettlementMethod]s from which the user can choose.
     */
    val settlementMethods = remember { SettlementMethod.sampleSettlementMethodsEmptyPrices }

    /**
     * The [SettlementMethod]s that the user has selected.
     */
    val selectedSettlementMethods = remember { mutableStateListOf<SettlementMethod>() }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp)
            .padding(PaddingValues(bottom = 10.dp))
    ) {
        Text(
            text = "Create Offer",
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Direction:",
            style =  MaterialTheme.typography.h6,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val isBuyDirection = (direction.value == OfferDirection.BUY)
            val isSellDirection = (direction.value == OfferDirection.SELL)
            DirectionButtonComposable(
                isSelected = isBuyDirection,
                text = "Buy",
                onClick = {
                    direction.value = OfferDirection.BUY
                },
                modifier = Modifier.width(195.dp)
            )
            Spacer(
                modifier = Modifier.width(10.dp)
            )
            DirectionButtonComposable(
                isSelected = isSellDirection,
                text = "Sell",
                onClick = {
                    direction.value = OfferDirection.SELL
                },
                modifier = Modifier.width(195.dp)
            )
        }
        Text(
            text = "Stablecoin:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinListComposable(
            chainID = chainID,
            stablecoins = stablecoins,
            selectedStablecoin = selectedStablecoin
        )
        /*
        If the user sets a minimum amount greater than a maximum amount, set the maximum amount equal to the minimum
        amount
         */
        if (minimumAmount.value > maximumAmount.value) {
            maximumAmount.value = minimumAmount.value
            maximumAmountString.value = maximumAmount.value.toString()
        }
        Text(
            text = "Minimum $stablecoinCurrencyCode Amount:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinAmountComposable(
            amountString = minimumAmountString,
            amount = minimumAmount
        )
        Text(
            text = "Maximum $stablecoinCurrencyCode Amount:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinAmountComposable(
            amountString = maximumAmountString,
            amount = maximumAmount
        )
        if (maximumAmount.value > securityDepositAmount.value * BigDecimal.TEN) {
            securityDepositAmount.value = maximumAmount.value.divide(BigDecimal.TEN)
                .setScale(6, RoundingMode.UP)
            securityDepositAmountString.value = securityDepositAmount.value.toString()
        }
        DisclosureComposable(
            header = {
                Text(
                    text = "Security Deposit $stablecoinCurrencyCode Amount:",
                    style =  MaterialTheme.typography.h6,
                )
            },
            content = {
                Text(
                    text = "The Security Deposit Amount must be at least 10% of the Maximum Amount."
                )
            }
        )
        StablecoinAmountComposable(
            amountString = securityDepositAmountString,
            amount = securityDepositAmount
        )
        DisclosureComposable(
            header = {
                Text(
                    text = "Service Fee Rate:",
                    style =  MaterialTheme.typography.h6,
                )
            },
            content = {
                Text(
                    text = "The percentage of the actual amount of stablecoin exchanged that you will be charged as " +
                            "a service fee."
                )
            }
        )
        if (offerTruthSource.isGettingServiceFeeRate.value) {
            Text(
                text = "Getting Service Fee Rate..."
            )
        } else {
            if (offerTruthSource.serviceFeeRate.value != null) {
                Text(
                    text = "${BigDecimal(offerTruthSource.serviceFeeRate.value)
                        .divide(BigDecimal.valueOf(100L)).setScale(2)} %",
                    style = MaterialTheme.typography.h4
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Could not get service fee rate"
                    )
                    Button(
                        onClick = {
                            offerTruthSource.updateServiceFeeRate()
                        },
                        content = {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.h5
                            )
                        },
                        border = BorderStroke(3.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor =  Color.Transparent,
                            contentColor = Color.Black,
                        ),
                        elevation = null,
                    )
                }
            }
        }
        Text(
            text = "Settlement Methods:",
            style =  MaterialTheme.typography.h6,
        )
        SettlementMethodSelector(
            settlementMethods = settlementMethods,
            stablecoinCurrencyCode = stablecoinCurrencyCode,
            selectedSettlementMethods = selectedSettlementMethods
        )
        Button(
            onClick = {},
            content = {
                Text(
                    text = "Create Offer",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            border = BorderStroke(3.dp, Color.Black),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =  Color.Transparent,
                contentColor = Color.Black,
            ),
            elevation = null,
            modifier = Modifier.width(400.dp)
        )
    }
}

/**
 * Displays a button that represents an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)
 * direction (Buy or Sell).
 *
 * @param isSelected Indicates whether this button represents the currently selected offer direction.
 * @param text The text to display on this button.
 * @param onClick The action to perform when this button is clicked.
 * @param modifier The [Modifier] to apply to this button.
 */
@Composable
fun DirectionButtonComposable(isSelected: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        content = {
            Text(
                text = text,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
        },
        border = if (!isSelected) BorderStroke(1.dp, Color.Black) else null,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color.Green else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black,
        ),
        modifier = modifier,
        elevation = null,
    )
}

/**
 * Displays a vertical list of Stablecoin cards, one for each stablecoin with the specified chain ID in the specified
 * [StablecoinInformationRepository]. Only one stablecoin card can be selected at a time.
 *
 * @param chainID The blockchain ID of the stablecoins to be displayed.
 * @param stablecoins A [StablecoinInformationRepository] from which this will obtain a list of stablecoins.
 * @param selectedStablecoin The contract address of the currently selected stablecoin, or null if the user has not
 * selected a stablecoin.
 */
@Composable
fun StablecoinListComposable(
    chainID: BigInteger,
    stablecoins: StablecoinInformationRepository,
    selectedStablecoin: MutableState<String?>
) {

    /**
     * Key-value pairs of stablecoin addresses and [StablecoinInformation] objects obtained from [stablecoins]
     */
    val stablecoinInformationEntries = (stablecoins.stablecoinInformation[chainID] ?: mapOf()).entries

    Column {
        for(stablecoinInfoListEntry in stablecoinInformationEntries) {
            val color = if (selectedStablecoin.value == stablecoinInfoListEntry.key) Color.Green else Color.Black
            Button(
                onClick = {
                    selectedStablecoin.value = stablecoinInfoListEntry.key
                },
                content = {
                    Text(
                        text = stablecoinInfoListEntry.value.name,
                        fontWeight = FontWeight.Bold
                    )
                },
                border = BorderStroke(1.dp, color),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp)
                    .padding(vertical = 5.dp),
                elevation = null
            )
        }
    }
}

/**
 * Displays a [TextField] customized to accept positive stablecoin amounts with up to six decimal places of precision.
 * The user cannot type characters that are not numbers or decimal points, and cannot type negative numbers.
 *
 * @param amountString The current amount as a [String].
 * @param amount The current amount as a [BigDecimal].
 */
@Composable
fun StablecoinAmountComposable(
    amountString: MutableState<String>,
    amount: MutableState<BigDecimal>
) {
    TextField(
        value = amountString.value,
        onValueChange = {
            if (it == "") {
                amount.value = BigDecimal.ZERO
                amountString.value = ""
            } else {
                try {
                    val newAmount = BigDecimal(it)
                    // Don't allow negative values, and limit to 6 decimal places
                    if (newAmount >= BigDecimal.ZERO && newAmount.scale() <= 6) {
                        amount.value = newAmount
                        amountString.value = it
                    }
                } catch (exception: NumberFormatException) {
                    /*
                    If the change prevents us from creating a BigDecimal minimum amount value, then we don't allow that
                    change
                     */
                }
            }
        },
        textStyle = MaterialTheme.typography.h5,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Displays a vertical list of settlement method cards, one for each settlement method that the offer has created. These
 * cards can be tapped to indicate that the user is willing to use them to send/receive payment for the offer being
 * created.
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

    Button(
        onClick = {
            if (isSelected.value) {
                selectedSettlementMethods.removeIf {
                    it.method == settlementMethod.method && it.currency == settlementMethod.currency
                }
                isSelected.value = false
            } else {
                selectedSettlementMethods.add(settlementMethod)
                isSelected.value = true
            }
        },
        content = {
            if (price.value == BigDecimal.ZERO) {
                settlementMethod.price = ""
            } else {
                settlementMethod.price = price.value.toString()
            }
            Row {
                Column(
                    horizontalAlignment = Alignment.Start
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
                                text = buildCreateOfferPriceDescription(settlementMethod, stablecoinCurrencyCode)
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
                Spacer(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        border = BorderStroke(1.dp, color),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = Color.Black
        ),
        modifier = Modifier.padding(PaddingValues(bottom = 6.dp)).width(400.dp),
        elevation = null,
    )
}

/**
 * Builds a human readable string describing the price specified for a settlement method, such as "Price: 0.94 EUR/DAI"
 * or "Price: 1.00 USD/USDC", or "Price: Tap to specify" if no price has been specified.
 *
 * @param settlementMethod The [SettlementMethod] for which this price description is being built.
 * @param stablecoinCurrencyCode The currency code of the stablecoin that the user has selected.
 */
fun buildCreateOfferPriceDescription(
    settlementMethod: SettlementMethod,
    stablecoinCurrencyCode: String,
): String {
    return if (settlementMethod.price == "") {
        "Price: Tap to specify"
    } else {
        "Price: ${settlementMethod.price} ${settlementMethod.currency}/${stablecoinCurrencyCode}"
    }
}

/**
 * Displays a preview of [CreateOfferComposable]
 */
@Preview
@Composable
fun PreviewCreateOfferComposable() {
    CreateOfferComposable(
        offerTruthSource = PreviewableOfferTruthSource(),
        chainID = BigInteger.ONE
    )
}