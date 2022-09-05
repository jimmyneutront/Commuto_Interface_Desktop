package com.commuto.interfacedesktop.ui.swap

import androidx.compose.runtime.mutableStateMapOf
import com.commuto.interfacedesktop.swap.FillingSwapState
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

/**
 * The Swap View Model, the single source of truth for all swap-related data. It is observed by swap-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @param swapService The [SwapService] used for completing the swap process for [Swap]s in [swaps].
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property swaps A mutable state map of [UUID]s to [Swap]s that acts as a single source of truth for all
 * offer-related data.
 */
class SwapViewModel @Inject constructor(private val swapService: SwapService): UISwapTruthSource {

    private val logger = LoggerFactory.getLogger(javaClass)

    override var swaps = mutableStateMapOf<UUID, Swap>().also { map ->
        Swap.sampleSwaps.map {
            map[it.id] = it
        }
    }

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    /**
     * Adds a new [Swap] to [swaps].
     *
     * @param swap The new [Swap] to be added to [swaps].
     */
    override fun addSwap(swap: Swap) {
        swaps[swap.id] = swap
    }

    /**
     * Sets the value of [Swap.fillingSwapState] of [swap] to [state] on the main coroutine dispatcher.
     *
     * @param swap The [Swap] of which to set the [Swap.fillingSwapState] value.
     * @param state The value to which [swap]'s [Swap.fillingSwapState] value will be set.
     */
    private suspend fun setFillingSwapState(swap: Swap, state: FillingSwapState) {
        withContext(Dispatchers.Main) {
            swap.fillingSwapState.value = state
        }
    }

    /**
     * Attempts to fill a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
     *
     * @param swap The [Swap] to fill.
     */
    override fun fillSwap(swap: Swap) {
        viewModelScope.launch {
            try {
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.CHECKING
                )
                logger.info("fillSwap: filling ${swap.id}")
                swapService.fillSwap(
                    swapToFill = swap,
                    afterPossibilityCheck = {
                        setFillingSwapState(
                            swap = swap,
                            state = FillingSwapState.APPROVING
                        )
                    },
                    afterTransferApproval = {
                        setFillingSwapState(
                            swap = swap,
                            state = FillingSwapState.FILLING
                        )
                    }
                )
                logger.info("fillSwap: successfully filled ${swap.id}")
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.error("fillSwap: got exception during fillSwap call for ${swap.id}", exception)
                swap.fillingSwapException = exception
                setFillingSwapState(
                    swap = swap,
                    state = FillingSwapState.EXCEPTION
                )
            }
        }
    }

}