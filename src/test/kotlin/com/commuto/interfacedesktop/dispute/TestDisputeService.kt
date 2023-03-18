package com.commuto.interfacedesktop.dispute

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException

/**
 * A basic [DisputeNotifiable] implementation used to satisfy [BlockchainService]'s disputeService dependency for
 * testing non-dispute-related code.
 */
class TestDisputeService: DisputeNotifiable {
    /**
     * Does nothing, required to adopt [DisputeNotifiable]. Should not be used.
     */
    override suspend fun handleFailedTransaction(
        transaction: BlockchainTransaction,
        exception: BlockchainTransactionException
    ) {}
}