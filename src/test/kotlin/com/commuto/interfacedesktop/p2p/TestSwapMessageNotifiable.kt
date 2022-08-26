package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.TakerInformationMessage

/**
 * A basic [SwapMessageNotifiable] implementation used to satisfy [P2PService]'s swapService dependency for testing
 * non-swap-related code.
 */
class TestSwapMessageNotifiable: SwapMessageNotifiable {
    /**
     * Does nothing, required to adopt [SwapMessageNotifiable]. Should not be used.
     */
    override suspend fun handleTakerInformationMessage(message: TakerInformationMessage) {}
}