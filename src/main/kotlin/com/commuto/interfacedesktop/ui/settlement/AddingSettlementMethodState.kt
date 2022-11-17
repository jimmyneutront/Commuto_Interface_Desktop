package com.commuto.interfacedesktop.ui.settlement

import com.commuto.interfacedesktop.settlement.SettlementMethod

/**
 * Indicates whether we are currently adding a new settlement method to the user's collection of settlement methods. If
 * we are, this indicates what part of the settlement-method-adding process we are currently in.
 *
 * @property NONE Indicates that we are not currently adding a settlement method.
 * @property SERIALIZING Indicates that we are currently attempting to serialize the private data corresponding to the
 * settlement method.
 * @property STORING Indicates that we are securely storing the settlement method and its private data in persistent
 * storage.
 * @property ADDING Indicates that we are adding the new [SettlementMethod] to the array of [SettlementMethod]s in the
 * settlement method truth source.
 * @property COMPLETED Indicates that the settlement-method-adding process finished successfully.
 * @property EXCEPTION Indicates that we encountered an exception while adding the settlement method.
 * @property description A human-readable string describing the current state.
 */
enum class AddingSettlementMethodState {
    NONE,
    SERIALIZING,
    STORING,
    ADDING,
    COMPLETED,
    EXCEPTION;

    val description: String
        get() = when (this) {
            // Note: This should not be used
            NONE -> "Press Add to add a settlement method"
            SERIALIZING -> "Serializing settlement method data..."
            STORING -> "Securely storing settlement method..."
            ADDING -> "Adding settlement method..."
            COMPLETED -> "Settlement method added."
            // Note: This should not be used; instead, the actual error message should be displayed.
            EXCEPTION -> "An exception occurred."
        }
}