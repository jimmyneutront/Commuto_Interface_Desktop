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

}