package com.commuto.interfacedesktop.dispute

import java.util.*

/**
 * A basic [DisputeTruthSource] implementation for testing.
 */
class TestDisputeTruthSource: DisputeTruthSource {
    var swapAndDisputes: MutableMap<UUID, SwapAndDispute> = mutableMapOf()
    override fun addSwapAndDispute(swapAndDispute: SwapAndDispute) {
        swapAndDisputes[swapAndDispute.id] = swapAndDispute
    }
}