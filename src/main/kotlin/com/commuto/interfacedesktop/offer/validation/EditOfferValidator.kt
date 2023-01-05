package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.EditingOfferState
import com.commuto.interfacedesktop.offer.Offer

/**
 * Ensures that [offer] can be canceled.
 *
 * This function ensures that:
 * - the user of this interface is the maker of [offer].
 * - [offer] is not take.
 * - [offer] has not been canceled.
 * - [offer] is not already being edited.
 *
 * @param offer The [Offer] to be validated for editing.
 *
 * @throws [OfferDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateOfferForEditing(offer: Offer) {
    if (!offer.isUserMaker) {
        throw OfferDataValidationException("You can only edit your own offers.")
    }
    if (offer.isTaken.value) {
        throw OfferDataValidationException("Offer ${offer.id} is already taken.")
    }
    if (!offer.isCreated.value) {
        throw OfferDataValidationException("Offer ${offer.id} is already canceled.")
    }
    if (offer.editingOfferState.value != EditingOfferState.VALIDATING) {
        throw OfferDataValidationException("Offer ${offer.id} is already being edited.")
    }
}