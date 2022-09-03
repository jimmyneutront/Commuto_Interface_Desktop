package com.commuto.interfacedesktop.swap

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * A basic [SwapTruthSource] implementation for testing.
 */
class TestSwapTruthSource: SwapTruthSource {
    override var swaps: SnapshotStateMap<UUID, Swap> = mutableStateMapOf()
    override fun addSwap(swap: Swap) {
        swaps[swap.id] = swap
    }
}