package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*

/**
 * A basic [OfferNotifiable] implementation used to satisfy [BlockchainService]'s offerService dependency for testing
 * non-offer-related code.
 */
class TestOfferService: OfferNotifiable {
    override suspend fun handleFailedTransaction(
        transaction: BlockchainTransaction,
        exception: BlockchainTransactionException
    ) {}
    override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {}
    override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {}
    override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {}
    override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {}
    override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {}
}