package com.commuto.interfacedesktop.ui.settlement

/**
 * Indicates whether we are currently deleting a settlement method belonging to the user. If we are, this indicates what
 * part of the settlement-method-deleting process we are currently in.
 *
 * @property NONE Indicates that we are not currently deleting a settlement method.
 * @property DELETING Indicates that we are currently deleting a settlement method.
 * @property COMPLETED Indicates that the settlement-method-deleting process finished successfully.
 * @property EXCEPTION Indicates that we encountered an exception while deleting the settlement method.
 * @property description A human-readable string describing the current state.
 */
enum class DeletingSettlementMethodState {
    NONE,
    DELETING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when (this) {
            // Note: This should not be used
            NONE -> "Press Delete to delete the settlement method"
            DELETING -> "Deleting settlement method..."
            COMPLETED -> "Settlement method deleted."
            // Note: This should not be used; instead, the actual error message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}