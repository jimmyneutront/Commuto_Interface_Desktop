package com.commuto.interfacedesktop.swap

import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * An interface that a class must implement in order to act as a single source of truth for swap-related data.
 * @property swaps A [SnapshotStateMap] mapping [UUID]s to [Swap]s that should act as a single source of truth for all
 * swap-related data.
 */
interface SwapTruthSource {
    var swaps: SnapshotStateMap<UUID, Swap>

    /**
     * Should add a new [Swap] to [swaps].
     *
     * @param swap The new [Swap] that should be added to [swaps].
     */
    fun addSwap(swap: Swap)
}