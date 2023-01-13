package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently taking an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer taking process we are currently in.
 *
 * @property NONE Indicates that we are not currently taking the corresponding offer.
 * @property VALIDATING Indicates that we are currently checking whether the corresponding offer can be taken.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will take the
 * corresponding offer.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will take the
 * corresponding offer, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have taken the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while taking the corresponding offer.
 * @property asString Returns a [String] corresponding to a particular case of [TakingOfferState].
 * @property description A human-readable string describing the current state.
 */
enum class TakingOfferState {
    NONE,
    VALIDATING,
    SENDING_TRANSACTION,
    AWAITING_TRANSACTION_CONFIRMATION,
    COMPLETED,
    EXCEPTION;

    val asString: String
        get() = when (this) {
            NONE -> "none"
            VALIDATING -> "validating"
            SENDING_TRANSACTION -> "sendingTransaction"
            AWAITING_TRANSACTION_CONFIRMATION -> "awaitingTransactionConfirmation"
            COMPLETED -> "completed"
            EXCEPTION -> "error"
        }

    val description: String
        get() = when(this) {
            NONE -> "Press Take Offer to take the offer"
            VALIDATING -> "Validating swap data..."
            SENDING_TRANSACTION -> "Taking the Offer..."
            AWAITING_TRANSACTION_CONFIRMATION -> "Waiting for confirmation..."
            COMPLETED -> "Offer successfully taken."
            EXCEPTION -> "An error occurred."
        }
}