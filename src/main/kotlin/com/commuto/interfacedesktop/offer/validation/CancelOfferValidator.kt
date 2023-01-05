package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.CancelingOfferState
import com.commuto.interfacedesktop.offer.Offer

/**
 * Ensures that [offer] can be canceled.
 *
 * This function ensures that:
 * - the user of this interface is the maker of [offer].
 * - [offer] is not taken.
 * - [offer] has not been canceled.
 * - [offer] is not already being canceled.
 *
 * @param offer The [Offer] to be validated for cancellation.
 *
 * @throws [OfferDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateOfferForCancellation(offer: Offer) {
    if (!offer.isUserMaker) {
        throw OfferDataValidationException("You can only cancel your own offers.")
    }
    if (offer.isTaken.value) {
        throw OfferDataValidationException("Offer ${offer.id} is taken and cannot be canceled.")
    }
    if (!offer.isCreated.value) {
        throw OfferDataValidationException("Offer ${offer.id} is already canceled")
    }
    if (offer.cancelingOfferState.value != CancelingOfferState.NONE &&
        offer.cancelingOfferState.value != CancelingOfferState.VALIDATING &&
        offer.cancelingOfferState.value != CancelingOfferState.EXCEPTION) {
        throw OfferDataValidationException("Offer ${offer.id} is already being canceled.")
    }
}