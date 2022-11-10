package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.settlement.SettlementMethod

/**
 * Validates edited settlement methods for an offer made by the user of this interface.
 *
 * This function ensures that:
 *  - the user has selected at least one settlement method
 *  - the user has specified a price for each selected settlement method
 *
 *  @param settlementMethods The [List] of [SettlementMethod]s to be validated.
 *
 *  @return A [List] of validated [SettlementMethod]s.
 *
 *  @throws [EditedSettlementMethodValidationException] if this is not able to ensure any of the conditions in the list
 *  above. The messages of the exceptions thrown by this function are human-readable and can be displayed to the user so
 *  that they can correct any problems.
 */
fun validateEditedSettlementMethods(
    settlementMethods: List<SettlementMethod>
): List<SettlementMethod> {
    if (settlementMethods.isEmpty()) {
        throw EditedSettlementMethodValidationException("You must specify at least one settlement method.")
    }
    settlementMethods.forEach {
        if (it.price == "") {
            throw EditedSettlementMethodValidationException("You must specify a price for each selected settlement " +
                    "method.")
        }
    }
    return settlementMethods
}