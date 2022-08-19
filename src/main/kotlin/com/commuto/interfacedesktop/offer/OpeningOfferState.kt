package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently opening a new offer. If we are, then this indicates the part of the
 * [offer opening process](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 * we are currently in.
 *
 * @property NONE Indicates that we are not currently opening a new offer.
 * @property VALIDATING Indicates that we are currently validating the user's submitted data for the new offer.
 * @property CREATING Indicates that we are currently creating a new key pair, ID, and Offer object for the new offer.
 * @property STORING Indicates that we are currently saving the new offer in persistent storage.
 * @property APPROVING Indicates that we are currently approving the token transfer for opening the new offer.
 * @property OPENING Indicates that we are currently calling CommutoSwap's
 * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) function for the new offer.
 * @property COMPLETED Indicates that we have successfully opened the new offer.
 * @property EXCEPTION Indicates that we encountered an exception during offer creation.
 * @property description A human-readable string describing the current state.
 */
enum class OpeningOfferState {
    NONE,
    VALIDATING,
    CREATING,
    STORING,
    APPROVING,
    OPENING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when(this) {
            NONE -> "Press Open Offer to open the offer" // Note: This should not be used.
            VALIDATING -> "Validating offer data..."
            CREATING -> "Creating a new key pair, offer ID and offer object..."
            STORING -> "Saving the new offer..."
            APPROVING -> "Approving token transfer..."
            OPENING -> "Opening the new offer..."
            COMPLETED -> "Offer successfully opened"
            /*
             Note: This should not be used; instead, the actual error message should be displayed
              */
            EXCEPTION -> "An exception occured."
        }

}