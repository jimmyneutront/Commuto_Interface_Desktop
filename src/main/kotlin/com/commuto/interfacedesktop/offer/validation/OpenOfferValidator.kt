package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferState
import com.commuto.interfacedesktop.offer.OpeningOfferState

/**
 * Ensures that [offer] can be opened.
 *
 * This function ensures that:
 * - the user of this interface is the maker of [offer].
 * - [offer] is in the [OfferState.AWAITING_OPENING] state.
 * - [offer] is not already being opened.
 *
 * @param offer The [Offer] to be validated for opening.
 *
 * @throws [OfferDataValidationException] if this is not able to ensure any of the conditions in the list above. The
 * descriptions of the exceptions thrown by this function are human-readable and can be displayed to the user so that
 * they can correct any problems.
 */
fun validateOfferForOpening(offer: Offer) {
    if (!offer.isUserMaker) {
        throw OfferDataValidationException(desc = "You can only open your own offers")
    }
    if (offer.state != OfferState.AWAITING_OPENING) {
        throw OfferDataValidationException(desc = "This Offer cannot currently be opened.")
    }
    if (offer.openingOfferState.value != OpeningOfferState.NONE &&
        offer.openingOfferState.value != OpeningOfferState.VALIDATING &&
        offer.openingOfferState.value != OpeningOfferState.EXCEPTION) {
        throw OfferDataValidationException(desc = "This Offer is already being opened.")
    }
}