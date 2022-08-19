package com.commuto.interfacedesktop.ui.swap

import androidx.compose.runtime.mutableStateMapOf
import com.commuto.interfacedesktop.swap.Swap
import java.util.*
import javax.inject.Inject

/**
 * The Swap View Model, the single source of truth for all swap-related data. It is observed by swap-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property swaps A mutable state map of [UUID]s to [Swap]s that acts as a single source of truth for all
 * offer-related data.
 */
class SwapViewModel @Inject constructor(): UISwapTruthSource {

    override var swaps = mutableStateMapOf<UUID, Swap>().also { map ->
        Swap.sampleSwaps.map {
            map[it.id] = it
        }
    }

    /**
     * Adds a new [Swap] to [swaps].
     *
     * @param swap The new [Swap] to be added to [swaps].
     */
    override fun addSwap(swap: Swap) {
        swaps[swap.id] = swap
    }
}