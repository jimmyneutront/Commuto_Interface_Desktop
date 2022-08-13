package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently canceling an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer cancellation process we are currently in. Note that this is only used for offers made by the user of
 * this interface.
 *
 * @property NONE Indicates that we are NOT currently canceling the corresponding offer.
 * @property CANCELING Indicates that we are currently canceling the corresponding offer.
 * @property COMPLETED Indicates that we have canceled the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while canceling the corresponding offer.
 */
enum class CancelingOfferState {
    NONE,
    CANCELING,
    COMPLETED,
    EXCEPTION;
}