package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.offer.OfferService
import java.math.BigInteger
import java.util.*
import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of swap-related events by [BlockchainService] and
 * [OfferService]
 */
@Singleton
interface SwapNotifiable {
    /**
     * The function called by [OfferService] in order to send a taker information message for an offer that has been
     * taken by the user of this interface.
     *
     * @param swapID The ID of the swap for which the structure or class adopting this protocol should announce taker
     * information.
     * @param chainID The ID of the blockchain on which the taken offer exists.
     */
    suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger)
}