package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import java.math.BigInteger

/**
 * Validated user-submitted data for a new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @property stablecoin The contract address of the stablecoin for which the new offer will be made.
 * @property stablecoinInformation The [StablecoinInformation] for the stablecoin for which the new offer will be made.
 * @property minimumAmount The minimum stablecoin amount for the new offer.
 * @property maximumAmount The maximum stablecoin amount for the new offer.
 * @property securityDepositAmount The security deposit amount for the new offer.
 * @property serviceFeeRate The current service fee rate, as a percentage times 100.
 * @property serviceFeeAmountLowerBound The minimum service fee for the new offer.
 * @property serviceFeeAmountUpperBound The maximum service fee for the new offer.
 * @property direction The direction for the new offer.
 * @property settlementMethods The settlement methods for the new offer.
 */
data class ValidatedNewOfferData(
    val stablecoin: String,
    val stablecoinInformation: StablecoinInformation,
    val minimumAmount: BigInteger,
    val maximumAmount: BigInteger,
    val securityDepositAmount: BigInteger,
    val serviceFeeRate: BigInteger,
    val serviceFeeAmountLowerBound: BigInteger,
    val serviceFeeAmountUpperBound: BigInteger,
    val direction: OfferDirection,
    val settlementMethods: List<SettlementMethod>
)