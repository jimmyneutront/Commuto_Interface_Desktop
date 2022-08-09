package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.commuto.interfacedesktop.ui.StablecoinInformation
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Displays the minimum and maximum service fee for an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param stablecoinInformation The [StablecoinInformation] for the offer's stablecoin, or `null` if no such information
 * is available.
 * @param minimumAmount The offer's minimum amount, as a [BigInteger] in ERC20 base units.
 * @param maximumAmount The offer's maximum amount, as a [BigInteger] in ERC20 base units.
 * @param serviceFeeRate The service fee rate of the offer, as a [BigInteger].
 */
@Composable
fun ServiceFeeAmountComposable(
    stablecoinInformation: StablecoinInformation?,
    minimumAmount: BigInteger,
    maximumAmount: BigInteger,
    serviceFeeRate: BigInteger
) {
    val headerString = if (stablecoinInformation != null) "Service Fee: " else "Service Fee (in token base units):"
    val currencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val minimumString = if (stablecoinInformation != null) (minimumAmount * serviceFeeRate).divide(BigInteger.TEN
        .pow(stablecoinInformation.decimal) * BigInteger.valueOf(10000L)).toString() else minimumAmount.toString()
    val maximumString = if (stablecoinInformation != null) (maximumAmount * serviceFeeRate).divide(BigInteger.TEN
        .pow(stablecoinInformation.decimal) * BigInteger.valueOf(10000L)).toString() else maximumAmount.toString()
    Text(
        text = headerString,
        style = MaterialTheme.typography.h6
    )
    if (minimumAmount == maximumAmount) {
        Text(
            text = "$minimumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
    } else {
        Text(
            text = "Minimum: $minimumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Maximum: $maximumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Displays the minimum and maximum service fee for an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param stablecoinInformation The [StablecoinInformation] for the offer's stablecoin, or `null` if no such information
 * is available.
 * @param minimumAmount The offer's minimum amount, as a [BigDecimal] in token units.
 * @param maximumAmount The offer's maximum amount, as a [BigDecimal] in token units.
 * @param serviceFeeRate The service fee rate of the offer, as a [BigInteger].
 */
@Composable
fun ServiceFeeAmountComposable(
    stablecoinInformation: StablecoinInformation?,
    minimumAmount: BigDecimal,
    maximumAmount: BigDecimal,
    serviceFeeRate: BigInteger
) {
    val headerString = "Service Fee:"
    val currencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val minimumString = (minimumAmount * BigDecimal(serviceFeeRate)).divide(BigDecimal.valueOf(10000L)).toString()
    val maximumString = (maximumAmount * BigDecimal(serviceFeeRate)).divide(BigDecimal.valueOf(10000L)).toString()
    Text(
        text = headerString,
        style = MaterialTheme.typography.h6
    )
    if (minimumAmount.compareTo(maximumAmount) == 0) {
        Text(
            text = "$minimumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
    } else {
        Text(
            text = "Minimum: $minimumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Maximum: $maximumString $currencyCode",
            style =  MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun PreviewServiceFeeAmountComposable() {
    ServiceFeeAmountComposable(
        stablecoinInformation = null,
        minimumAmount = BigDecimal.valueOf(1L),
        maximumAmount = BigDecimal.valueOf(2L),
        serviceFeeRate = BigInteger.valueOf(100)
    )
}