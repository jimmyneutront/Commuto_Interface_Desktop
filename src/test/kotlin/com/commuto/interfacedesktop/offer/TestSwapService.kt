package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.swap.SwapNotifiable
import java.math.BigInteger
import java.util.*

/**
 * A basic [SwapNotifiable] implementation used to satisfy [OfferService]'s swapService dependency for testing
 * non-swap-related code.
 */
class TestSwapService: SwapNotifiable {
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {}
}