package com.commuto.interfacedesktop.ui.swap

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.commuto.interfacedesktop.swap.Swap
import java.util.*

/**
 * A [UISwapTruthSource] implementation used for previewing user interfaces.
 *
 * @property swaps A [SnapshotStateMap] mapping  [UUID]s to [Swap]s, which acts as a single source of truth for all
 * swap-related data.
 */
class PreviewableSwapTruthSource : UISwapTruthSource {
    override var swaps = SnapshotStateMap<UUID, Swap>().also { map ->
        Swap.sampleSwaps.map {
            map[it.id] = it
        }
    }

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun addSwap(swap: Swap) {}

    /**
     * Not used since this class is for previewing user interfaces, but required for adoption of [UISwapTruthSource].
     */
    override fun fillSwap(swap: Swap) {}

    /**
     * Not used since this class is for previewing user interfaces, but required for adoption of [UISwapTruthSource].
     */
    override fun reportPaymentSent(swap: Swap) {}
}