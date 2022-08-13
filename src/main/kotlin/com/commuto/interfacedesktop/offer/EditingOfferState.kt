package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently editing an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer editing process we are currently in. Note that this is only used for offers made by the user of this
 * interface.
 *
 * @property NONE Indicates that we are NOT currently editing the corresponding offer.
 * @property EDITING Indicates that we are currently editing the corresponding offer.
 * @property COMPLETED Indicates that we have edited the corresponding offer.
 * @property EXCEPTION Indicates that we encountered an exception while editing the corresponding offer.
 */
enum class EditingOfferState {
    NONE,
    EDITING,
    COMPLETED,
    EXCEPTION;
}