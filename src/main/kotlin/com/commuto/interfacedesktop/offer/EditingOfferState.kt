package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently editing an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If we are, then this indicates the part
 * of the offer editing process we are currently in. Note that this is only used for offers made by the user of this
 * interface.
 *
 * @property NONE Indicates that the corresponding offer is not currently being edited.
 * @property EDITING Indicates that the corresponding offer is currently being edited.
 * @property COMPLETED Indicates that the corresponding offer has been edited.
 * @property EXCEPTION Indicates that an error was encountered during offer editing.
 */
enum class EditingOfferState {
    NONE,
    EDITING,
    COMPLETED,
    EXCEPTION;
}