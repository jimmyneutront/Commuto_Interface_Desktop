package com.commuto.interfacedesktop.dispute

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * A basic [DisputeTruthSource] implementation for testing.
 */
class TestDisputeTruthSource: DisputeTruthSource {
    override var swapAndDisputes: SnapshotStateMap<UUID, SwapAndDispute> = mutableStateMapOf()
    override fun addSwapAndDispute(swapAndDispute: SwapAndDispute) {
        swapAndDisputes[swapAndDispute.id] = swapAndDispute
    }
}