package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService
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
    override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean { return false }
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handleFailedTransaction(
        transaction: BlockchainTransaction,
        exception: BlockchainTransactionException
    ) {}
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handleNewSwap(takenOffer: Offer) {}
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}

    /**
     * Does nothing, required to adopt [SwapNotifiable]. Should not be used.
     */
    override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
}