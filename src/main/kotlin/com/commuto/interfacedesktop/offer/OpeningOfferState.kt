package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently opening a new offer. If we are, then this indicates the part of the
 * [offer opening process](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 * we are currently in.
 *
 * @property NONE Indicates that we are NOT currently opening the corresponding offer.
 * @property VALIDATING Indicates that we are currently checking whether the corresponding offer can be opened.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will open the
 * corresponding offer.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will open the
 * corresponding offer, and we are waiting for it to be confirmed.
 * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) function for the new offer.
 * @property COMPLETED Indicates that we have opened the corresponding offer, and the transaction that did so has been
 * confirmed
 * @property EXCEPTION Indicates that we encountered an exception while opening the corresponding offer.
 * @property asString Returns a [String] corresponding to a particular case of [OpeningOfferState].
 * @property description A human-readable string describing the current state.
 */
enum class OpeningOfferState {
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
            NONE -> "Press Open Offer to open the offer" // Note: This should not be used.
            VALIDATING -> "Validating offer data..."
            SENDING_TRANSACTION -> "Opening the Offer..."
            AWAITING_TRANSACTION_CONFIRMATION -> "Waiting for confirmation..."
            COMPLETED -> "Offer successfully opened."
            /*
            Note: This should not be used; instead, the actual error message should be displayed
             */
            EXCEPTION -> "An exception occurred."
        }

}