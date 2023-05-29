package com.commuto.interfacedesktop.dispute

import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * An interface that a class must implement in order to act as a single source of truth for dispute-related data.
 * @property swapAndDisputes A [SnapshotStateMap] mapping [UUID]s to [SwapAndDispute]s that should act as a single
 * source of truth for all data related to disputed swaps for which the user of this interface is a dispute agent.
 */
interface DisputeTruthSource {
    var swapAndDisputes: SnapshotStateMap<UUID, SwapAndDispute>

    /**
     * Should add a new [SwapAndDispute] to a list.
     *
     * @param swapAndDispute The new [SwapAndDispute] that should be added to a list.
     */
    fun addSwapAndDispute(swapAndDispute: SwapAndDispute)

}