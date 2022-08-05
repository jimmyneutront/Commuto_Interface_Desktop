package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently canceling an offer. If we are, then this indicates the part of the offer
 * cancellation process we are currently in. Note that this is only used for offers made by the user of this interface.
 *
 * @property NONE Indicates that the corresponding offer is open and not currently being canceled.
 * @property CANCELING Indicates that the corresponding offer is currently being canceled.
 * @property COMPLETED Indicates that the corresponding offer has been canceled.
 * @property EXCEPTION Indicates that an exception was encountered during offer cancellation.
 */
enum class CancelingOfferState {
    NONE,
    CANCELING,
    COMPLETED,
    EXCEPTION;
}