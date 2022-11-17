package com.commuto.interfacedesktop.ui.settlement

import com.commuto.interfacedesktop.settlement.SettlementMethod

/**
 * Indicates whether we are currently editing a settlement method belonging to the user. If we are, this indicates what
 * part of the settlement-method-editing process we are currently in.
 *
 * @property NONE Indicates that we are not currently editing a settlement method.
 * @property SERIALIZING Indicates that we are currently attempting to serialize new private data that will replace the
 * current private data of the settlement method.
 * @property STORING Indicates that we are securely replacing the settlement method's existing private data with new
 * private data in persistent storage.
 * @property EDITING Indicates that we are editing the private data of the [SettlementMethod] in the settlement method
 * truth source.
 * @property COMPLETED Indicates that the settlement-method-editing process finished successfully.
 * @property EXCEPTION Indicates that we encountered an exception while editing the settlement method.
 * @property description A human-readable string describing the current state.
 */
enum class EditingSettlementMethodState {
    NONE,
    SERIALIZING,
    STORING,
    EDITING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when (this) {
            // Note: This should not be used
            NONE -> "Press Edit to edit the settlement method"
            SERIALIZING -> "Serializing settlement method data..."
            STORING -> "Securely storing settlement method data..."
            EDITING -> "Editing settlement method..."
            COMPLETED -> "Settlement method edited."
            // Note: This should not be used; instead, the actual exception message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}