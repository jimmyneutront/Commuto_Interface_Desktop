package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.swap.Swap

/**
 * Indicates whether we are currently taking an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer taking process we are currently in.
 *
 * @property NONE Indicates that we are not currently taking the corresponding offer.
 * @property VALIDATING Indicates that we are currently validating user-submitted data for taking the corresponding
 * offer.
 * @property CHECKING Indicates that we are currently checking if the offer is open and not taken.
 * @property CREATING Indicates that we are currently creating a new key pair and [Swap] object for the new swap.
 * @property STORING Indicates that we are currently saving the new swap in persistent storage.
 * @property APPROVING Indicates that we are currently approving the token transfer to take the corresponding offer.
 * @property TAKING Indicates that we are currently calling CommutoSwap's
 * [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer) function for the new offer.
 * @prop
 * @property COMPLETED Indicates that we have taken the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while taking the corresponding offer.
 * @property description A human-readable string describing the current state.
 */
enum class TakingOfferState {
    NONE,
    VALIDATING,
    CHECKING,
    CREATING,
    STORING,
    APPROVING,
    TAKING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when(this) {
            NONE -> "Press Take Offer to take the offer"
            VALIDATING -> "Validating swap data..."
            CHECKING -> "Checking offer availability..."
            CREATING -> "Creating a new key pair and swap object..."
            STORING -> "Saving the new swap..."
            APPROVING -> "Approving token transfer..."
            TAKING -> "Taking the offer..."
            COMPLETED -> "Offer successfully taken"
            EXCEPTION -> "An error occurred."
        }
}