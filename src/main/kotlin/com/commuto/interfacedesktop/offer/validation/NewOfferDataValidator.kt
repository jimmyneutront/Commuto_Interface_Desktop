package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Validates the data that the user has specified for the creation of a new offer, so that the offer creation
 * transaction does not revert.
 *
 * This funciton ensures that:
 * - the user has selected a valid stablecoin
 * - the minimum stablecoin amount is greater than zero
 * - the maximum stablecoin amount is greater than or equal to the minimum amount
 * - the security deposit amount is at least 10% of the maximum stablecoin amount
 * - the minimum service fee amount is greater than zero
 * - the user has specified a valid direction
 * - the user has selected at least one settlement method
 * - the user has specified a price for each selected settlement method.
 *
 * @param stablecoin The contract address of the stablecoin selected by the user, or `null` if the user has not selected
 * a stablecoin.
 * @param stablecoinInformation The [StablecoinInformation] for the stablecoin selected by the user, or `null` if the
 * user has not selected a stablecoin.
 * @param minimumAmount The minimum stablecoin amount to be exchanged, as specified by the user.
 * @param maximumAmount The maximum stablecoin amount to be exchanged, as specified by the user.
 * @param securityDepositAmount The security deposit amount, as specified by the user.
 * @param serviceFeeRate The current [service fee rate]
 * (https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt).
 * @param direction The exchange direction indicated by the user, either `BUY` or `SELL`.
 * @param settlementMethods The [SettlementMethod]s selected by the user.
 *
 * @return A [ValidatedNewOfferData] derived from the inputs to this function.
 *
 * @throws [NewOfferDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the errors thrown by this function are human-readable and can be displayed to the user so that they
 * can correct any problems.
 */
fun validateNewOfferData(
    stablecoin: String?,
    stablecoinInformation: StablecoinInformation?,
    minimumAmount: BigDecimal,
    maximumAmount: BigDecimal,
    securityDepositAmount: BigDecimal,
    serviceFeeRate: BigInteger,
    direction: OfferDirection?,
    settlementMethods: List<SettlementMethod>
): ValidatedNewOfferData {
    if (stablecoin == null || stablecoinInformation == null) {
        throw NewOfferDataValidationException("You must select a valid stablecoin.")
    }
    val minimumAmountBaseUnits = stablecoinAmountToBaseUnits(minimumAmount, stablecoinInformation)
    if (minimumAmountBaseUnits <= BigInteger.ZERO) {
        throw NewOfferDataValidationException("Minimum amount must be greater than zero.")
    }
    val maximumAmountBaseUnits = stablecoinAmountToBaseUnits(maximumAmount, stablecoinInformation)
    if (maximumAmountBaseUnits < minimumAmountBaseUnits) {
        throw NewOfferDataValidationException("Maximum amount must not be less than the minimum amount.")
    }
    val securityDepositAmountBaseUnits = stablecoinAmountToBaseUnits(securityDepositAmount, stablecoinInformation)
    if (securityDepositAmountBaseUnits.times(BigInteger.TEN) < maximumAmountBaseUnits) {
        throw NewOfferDataValidationException("The security deposit amount must be at least 10% of the maximum amount.")
    }
    val serviceFeeAmountLowerBound = serviceFeeRate * (minimumAmountBaseUnits / BigInteger.valueOf(10_000L))
    if (serviceFeeAmountLowerBound <= BigInteger.ZERO) {
        throw NewOfferDataValidationException("The minimum service fee amount must be greater than zero.")
    }
    val serviceFeeAmountUpperBound = serviceFeeRate * (maximumAmountBaseUnits / BigInteger.valueOf(10_000L))
    if (settlementMethods.isEmpty()) {
        throw NewOfferDataValidationException("You must specify at least one settlement method.")
    }
    if (direction == null) {
        throw NewOfferDataValidationException("You must specify a direction.")
    }
    settlementMethods.forEach {
        if (it.price == "") {
            throw NewOfferDataValidationException("You must specify a price for each settlement method.")
        }
    }
    return ValidatedNewOfferData(
        stablecoin = stablecoin,
        stablecoinInformation = stablecoinInformation,
        minimumAmount = minimumAmountBaseUnits,
        maximumAmount = maximumAmountBaseUnits,
        securityDepositAmount = securityDepositAmountBaseUnits,
        serviceFeeRate = serviceFeeRate,
        serviceFeeAmountLowerBound = serviceFeeAmountLowerBound,
        serviceFeeAmountUpperBound = serviceFeeAmountUpperBound,
        direction = direction,
        settlementMethods = settlementMethods
    )
}

/**
 * Converts a stablecoin amount as a [BigDecimal] to a base unit amount as a [BigInteger], according to the decimal
 * value of [stablecoinInformation]. So for example, consider USDC with a decimal value of 6: `BigDecimal(1)` USDC
 * becomes `BigInteger("1_000_000")` USDC in base units.
 *
 * @param amount The [BigDecimal] amount to be converted to token base units.
 * @param stablecoinInformation The [StablecoinInformation] from which this gets the decimal value to use when
 * converting [amount].
 *
 * @return A [BigInteger] which is [amount] in token base units.
 */
fun stablecoinAmountToBaseUnits(amount: BigDecimal, stablecoinInformation: StablecoinInformation): BigInteger {
    val amountDecimalBaseUnits = amount.times(BigDecimal.TEN.pow(stablecoinInformation.decimal))
        .setScale(0, RoundingMode.CEILING)
    return amountDecimalBaseUnits.toBigInteger()
}