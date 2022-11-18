package com.commuto.interfacedesktop.ui.settlement

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData

interface UISettlementMethodTruthSource {
    val settlementMethods: SnapshotStateList<SettlementMethod>

    /**
     * Adds a given [SettlementMethod] with corresponding [PrivateData] to the collection of the user's settlement
     * methods.
     *
     * @param settlementMethod The new [SettlementMethod] that the user wants added to their collection of settlement
     * methods.
     * @param newPrivateData Some [PrivateData] corresponding to [settlementMethod], supplied by the user.
     * @param stateOfAdding A [MutableState] wrapped around an [AddingSettlementMethodState] value, describing the
     * current state of the settlement-method-adding process.
     * @param addSettlementMethodException A [MutableState] around an optional [Exception], the wrapped value of which
     * this will set equal to the exception that occurs in the settlement-method-adding process, if any.
     */
    fun addSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        stateOfAdding: MutableState<AddingSettlementMethodState>,
        addSettlementMethodException: MutableState<Exception?>
    )

    /**
     * Edits a given [SettlementMethod], replacing its current private data with the supplied [PrivateData] in the
     * collection of the user's settlement methods.
     *
     * @param settlementMethod The user's [SettlementMethod] that they want to edit.
     * @param newPrivateData Some [PrivateData], with which the current private data of the given settlement method will
     * be replaced.
     * @param stateOfEditing A [MutableState] wrapped around an [EditingSettlementMethodState] value, describing the
     * current state of the settlement-method-editing process.
     * @param editSettlementMethodException A [MutableState] around an optional [Exception], the wrapped value of which
     * this will set equal to the exception that occurs in the settlement-method-editing process, if any.
     * @param privateDataMutableState A [MutableState] around an optional [PrivateData], with which this will update
     * with [newPrivateData] once the settlement-method-editing process is complete.
     */
    fun editSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        stateOfEditing: MutableState<EditingSettlementMethodState>,
        editSettlementMethodException: MutableState<Exception?>,
        privateDataMutableState: MutableState<PrivateData?>
    )

    /**
     * Deletes a given [SettlementMethod].
     *
     * @param settlementMethod The user's [SettlementMethod] that they want to delete.
     * @param stateOfDeleting A [MutableState] wrapped around a [DeletingSettlementMethodState] value, describing the
     * current state of the settlement-method-deleting process.
     * @param deleteSettlementMethodException A [MutableState] around an optional [Exception], the wrapped value of
     * which this will set equal to the exception that occurs in the settlement-method-deleting process, if any.
     */
    fun deleteSettlementMethod(
        settlementMethod: SettlementMethod,
        stateOfDeleting: MutableState<DeletingSettlementMethodState>,
        deleteSettlementMethodException: MutableState<Exception?>,
    ) {}

}