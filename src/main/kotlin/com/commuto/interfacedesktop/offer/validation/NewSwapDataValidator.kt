package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import com.commuto.interfacedesktop.settlement.privatedata.createPrivateDataObject
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import java.math.BigDecimal

/**
 * Validates the data that the user has specified while attempting to take an offer, so that the offer taking
 * transaction does not revert.
 *
 * This function ensures that:
 *  - the isUserMaker field is false (meaning the user is not the maker of the offer).
 *  - the havePublicKey field is true (meaning this interface has a copy of the maker's public key).
 *  - (if the offer's lower and upper bound amounts are not equal) the takenSwapAmount is within the bounds specified by
 *  the offer maker.
 *  - the user has selected a settlement method accepted by the maker.
 *  - the user has selected one of their own settlement methods that is compatible with the maker's settlement method
 *  that the user has selected
 *  - the user has supplied their information for the selected settlement method, and the information is valid.
 *
 *  @param offer The [Offer] that is being taken.
 *  @param takenSwapAmount (If the lower and upper bound amounts of [offer] are equal, this will not be used.) The
 *  stablecoin amount that the user wants to buy/sell as a [BigDecimal]
 *  @param selectedMakerSettlementMethod One of the settlement methods specified by the maker, by which the user wants
 *  to send/receive payment to/from the maker.
 *  @param selectedTakerSettlementMethod One of the user's settlement methods, that has the same method and currency
 *  properties as [selectedMakerSettlementMethod], and contains the user's private information that will be sent to the
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
    selectedMakerSettlementMethod: SettlementMethod?,
    selectedTakerSettlementMethod: SettlementMethod?,
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
    if (selectedMakerSettlementMethod == null) {
        throw NewSwapDataValidationException("You must select a settlement method accepted by the offer maker.")
    }
    if (selectedTakerSettlementMethod == null) {
        throw NewSwapDataValidationException("You must select one of your settlement methods.")
    }
    if (selectedMakerSettlementMethod.method != selectedTakerSettlementMethod.method ||
        selectedMakerSettlementMethod.currency != selectedTakerSettlementMethod.currency) {
        throw NewSwapDataValidationException("You must select a settlement method accepted by the offer maker.")
    }
    val privateData = selectedTakerSettlementMethod.privateData
        ?: throw NewSwapDataValidationException("You must supply your information for your settlement method.")
    val privateDataObject = createPrivateDataObject(privateData = privateData)
        ?: throw NewSwapDataValidationException("Your settlement method has invalid details")
    if (privateDataObject is PrivateSEPAData) {
        if (selectedMakerSettlementMethod.method != "SEPA") {
            throw NewSwapDataValidationException("You must select a settlement method accepted by the offer maker.")
        }
    } else if (privateDataObject is PrivateSWIFTData) {
        if (selectedMakerSettlementMethod.method != "SWIFT") {
            throw NewSwapDataValidationException("You must select a settlement method accepted by the offer maker.")
        }
    } else {
        throw NewSwapDataValidationException("You must select a supported settlement method")
    }
    val selectedMakerSettlementMethodInArray = offer.settlementMethods.firstOrNull {
        it.currency == selectedMakerSettlementMethod.currency && it.price == selectedMakerSettlementMethod.price &&
                it.method == selectedMakerSettlementMethod.method
    } ?: throw NewSwapDataValidationException("The user does not use the settlement method you selected.")
    return ValidatedNewSwapData(
        takenSwapAmount = takenSwapAmountBaseUnits,
        makerSettlementMethod = selectedMakerSettlementMethodInArray,
        takerSettlementMethod = selectedTakerSettlementMethod,
    )
}