package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently canceling an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer cancellation process we are currently in. Note that this is only used for offers made by the user of
 * this interface.
 *
 * @property NONE Indicates that we are NOT currently canceling the corresponding offer.
 * @property VALIDATING Indicates that we are currently checking whether the corresponding offer can be canceled.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will cancel the
 * corresponding offer.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will cancel the
 * corresponding offer, and are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have canceled the corresponding offer, and the transaction that did so has been
 * confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while canceling the corresponding offer.
 */
enum class CancelingOfferState {
    NONE,
    VALIDATING,
    SENDING_TRANSACTION,
    AWAITING_TRANSACTION_CONFIRMATION,
    COMPLETED,
    EXCEPTION;
}