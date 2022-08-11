package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.commuto.interfacedesktop.ui.StablecoinInformation
import java.math.BigInteger

/**
 * Displays the minimum and maximum amount stablecoin that the maker of an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)  is willing to exchange.
 *
 * @param stablecoinInformation A [StablecoinInformation?] for this offer's stablecoin.
 * @param min The Offer's minimum amount.
 * @param max The Offer's maximum amount.
 * @param securityDeposit The Offer's security deposit amount.
 */
@Composable
fun OfferAmountComposable(
    stablecoinInformation: StablecoinInformation?,
    min: BigInteger,
    max: BigInteger,
    securityDeposit: BigInteger
) {
    val headerString = if (stablecoinInformation != null) "Amount: " else "Amount (in token base units):"
    val currencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val minimumString = if (stablecoinInformation != null) min.divide(BigInteger.TEN.pow(stablecoinInformation.decimal))
        .toString() else min.toString()
    val maximumString = if (stablecoinInformation != null) max.divide(BigInteger.TEN.pow(stablecoinInformation.decimal))
        .toString() else min.toString()
    val securityDepositString = if (stablecoinInformation != null) securityDeposit.divide(
        BigInteger.TEN
        .pow(stablecoinInformation.decimal)).toString() else securityDeposit.toString()
    Text(
        text = headerString,
        style = MaterialTheme.typography.h6
    )
    if (min == max) {
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
    Text(
        text = "Security Deposit: $securityDepositString $currencyCode",
        style = MaterialTheme.typography.h5,
    )
}

/**
 * Displays a preview of [OfferAmountComposable]
 */
@Preview
@Composable
fun PreviewOfferAmountComposable() {
    OfferAmountComposable(
        stablecoinInformation = null,
        min = BigInteger.valueOf(10_000L),
        max = BigInteger.valueOf(20_000L),
        securityDeposit = BigInteger.valueOf(100L)
    )
}