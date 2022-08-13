package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently taking an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer taking process we are currently in.
 *
 * @property NONE Indicates that we are not currently taking the corresponding offer.
 * @property VALIDATING Indicates that we are currently validating user-submitted data for taking the corresponding
 * offer.
 * @property COMPLETED Indicates that we have taken the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while taking the corresponding offer.
 * @property description A human-readable string describing the current state.
 */
enum class TakingOfferState {
    NONE,
    VALIDATING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when(this) {
            NONE -> "Press Take Offer to take the offer"
            VALIDATING -> "Validating swap data..."
            COMPLETED -> "Offer successfully taken"
            EXCEPTION -> "An error occurred."
        }
}