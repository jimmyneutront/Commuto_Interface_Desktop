package com.commuto.interfacedesktop.ui.settlement

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData

/**
 * A [UISettlementMethodTruthSource] implementation used for previewing user interfaces.
 * @property settlementMethods A [SnapshotStateList] of containing the user's [SettlementMethod]s.
 */
class PreviewableSettlementMethodTruthSource: UISettlementMethodTruthSource {
    override var settlementMethods = SnapshotStateList<SettlementMethod>().also { snapshotStateList ->
        SettlementMethod.sampleSettlementMethodsEmptyPrices.map {
            snapshotStateList.add(it)
        }
    }

    /**
     * Not used since this class is for previewing user interfaces, but required for implementation of
     * [UISettlementMethodTruthSource].
     */
    override fun addSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        stateOfAdding: MutableState<AddingSettlementMethodState>,
        addSettlementMethodException: MutableState<Exception?>
    ) {}

    /**
     * Not used since this class is for previewing user interfaces, but required for adoption of
     * [UISettlementMethodTruthSource].
     */
    override fun editSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        stateOfEditing: MutableState<EditingSettlementMethodState>,
        editSettlementMethodException: MutableState<Exception?>,
        privateDataMutableState: MutableState<PrivateData?>
    ) {}
}