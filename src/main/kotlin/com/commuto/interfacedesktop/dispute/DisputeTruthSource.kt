package com.commuto.interfacedesktop.dispute

/**
 * An interface that a class must implement in order to act as a single source of truth for dispute-related data.
 */
interface DisputeTruthSource {

    /**
     * Should add a new [SwapAndDispute] to a list.
     *
     * @param swapAndDispute The new [SwapAndDispute] that should be added to a list.
     */
    fun addSwapAndDispute(swapAndDispute: SwapAndDispute)

}