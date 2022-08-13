package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import java.math.BigDecimal

/**
 * Validates the data that the user has specified while attempting to take an offer, so that the offer taking
 * transaction does not revert.
 *
 * This function ensures that:
 *  - the isUserMaker field is false (meaning the user is not the maker of the offer)
 *  - the havePublicKey field is true (meaning this interface has a copy of the maker's public key)
 *  - (if the offer's lower and upper bound amounts are not equal) the takenSwapAmount is within the bounds specified by
 *  the offer maker
 *  - the user has selected a settlement method
 *  - the settlement method selected by the user is accepted by the offer maker
 *
 *  @param offer The [Offer] that is being taken.
 *  @param takenSwapAmount (If the lower and upper bound amounts of [offer] are equal, this will not be used.) The
 *  stablecoin amount that the user wants to buy/sell as a [BigDecimal]
 *  @param selectedSettlementMethod The settlement method by which the user wants to send/receive payment to/from the
 *  maker.
 *  @param stablecoinInformationRepository The [StablecoinInformationRepository] that this will use to convert
 *  [takenSwapAmount] to ERC20 base units.
 *
 *  @return A [ValidatedNewSwapData] derived from the inputs to this function.
 *
 *  @throws [NewSwapDataValidationException] if this is not able to ensure any of he conditions in the list above. The
 *  descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 *  they can correct any problems.
 */
fun validateNewSwapData(
    offer: Offer,
    takenSwapAmount: BigDecimal,
    selectedSettlementMethod: SettlementMethod?,
    stablecoinInformationRepository: StablecoinInformationRepository
): ValidatedNewSwapData {
    if (offer.isUserMaker) {
        throw NewSwapDataValidationException("You cannot take your own offer.")
    }
    if (!offer.havePublicKey) {
        throw NewSwapDataValidationException("The maker's public key has not yet been obtained.")
    }
    val stablecoinInformation = stablecoinInformationRepository.getStablecoinInformation(
        chainID = offer.chainID,
        contractAddress = offer.stablecoin
    ) ?: throw NewSwapDataValidationException("Could not validate stablecoin contract address.")
    val takenSwapAmountBaseUnits = if (offer.amountLowerBound == offer.amountUpperBound) offer.amountLowerBound else
        stablecoinAmountToBaseUnits(amount = takenSwapAmount, stablecoinInformation = stablecoinInformation)
    if (offer.amountLowerBound > takenSwapAmountBaseUnits) {
        throw NewSwapDataValidationException("You must specify a stablecoin amount that is not less than the minimum " +
                "amount.")
    }
    if (offer.amountUpperBound < takenSwapAmountBaseUnits) {
        throw NewSwapDataValidationException("You must specify a stablecoin amount that is not greater than the " +
                "maximum amount.")
    }
    if (selectedSettlementMethod == null) {
        throw NewSwapDataValidationException("You must select a settlement method.")
    }
    val selectedSettlementMethodInArray = offer.settlementMethods.firstOrNull {
        it.currency == selectedSettlementMethod.currency && it.price == selectedSettlementMethod.price &&
                it.method == selectedSettlementMethod.method
    } ?: throw NewSwapDataValidationException("The user does not use the settlement method you selected.")
    return ValidatedNewSwapData(
        takenSwapAmount = takenSwapAmountBaseUnits,
        settlementMethod = selectedSettlementMethodInArray
    )
}