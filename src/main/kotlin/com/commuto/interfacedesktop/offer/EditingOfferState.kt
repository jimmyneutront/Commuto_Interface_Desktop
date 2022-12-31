package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently editing an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer editing process we are currently in. Note that this is only used for offers made by the user of this
 * interface.
 *
 * @property NONE Indicates that we are NOT currently editing the corresponding offer.
 * @property VALIDATING Indicates that we are currently checking whether the corresponding offer can be edited.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will edit the
 * corresponding offer.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will edit the
 * corresponding offer, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have edited the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while editing the corresponding offer.
 * @property asString A [String] corresponding to a particular case of [EditingOfferState].
 */
enum class EditingOfferState {
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

}