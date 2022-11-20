package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import com.commuto.interfacedesktop.settlement.privatedata.createPrivateDataObject

/**
 * Validates edited settlement methods for an offer made by the user of this interface.
 *
 * This function ensures that:
 * - the user has selected at least one settlement method
 * - the user has specified a price for each selected settlement method
 * - the user has supplied their information for each selected settlement method.
 * - the information that the user has supplied for each settlement method is valid.
 * - the type of information that the user has supplied for each settlement method corresponds to that type of
 * settlement method.
 *
 * @param settlementMethods The [List] of [SettlementMethod]s to be validated.
 *
 * @return An [Array] of validated [SettlementMethod]s.
 *
 * @throws [SettlementMethodValidationException] if this is not able to ensure any of the conditions in the list above.
 * The descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so
 * that they can correct any problems.
 */
fun validateSettlementMethods(settlementMethods: List<SettlementMethod>): List<SettlementMethod> {
    if (settlementMethods.isEmpty()) {
        throw SettlementMethodValidationException("You must specify at least one settlement method.")
    }
    settlementMethods.forEach {
        if (it.price == "") {
            throw SettlementMethodValidationException("You must specify a price for each settlement method you select.")
        }
        val privateData = it.privateData ?: throw SettlementMethodValidationException(
            "You must supply your " +
                    "information for each settlement method you select."
        )
        val privateDataObject = createPrivateDataObject(privateData = privateData)
            ?: throw SettlementMethodValidationException("Your ${it.currency} ${it.method} has invalid details")
        if (privateDataObject is PrivateSEPAData) {
            if (it.method != "SEPA") {
                throw SettlementMethodValidationException(
                    "You cannot not supply SEPA details for a ${it.method} " +
                            "settlement method."
                )
            }
        } else if (privateDataObject is PrivateSWIFTData) {
            if (it.method != "SWIFT") {
                throw SettlementMethodValidationException(
                    "You cannot not supply SWIFT details for a ${it.method} " +
                            "settlement method."
                )
            }
        } else {
            throw SettlementMethodValidationException("You must select a supported settlement method")
        }
    }
    return settlementMethods
}