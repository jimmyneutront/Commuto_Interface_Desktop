package com.commuto.interfacedesktop.dispute

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException
import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of dispute-related events by [BlockchainService].
 */
@Singleton
interface DisputeNotifiable {
    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in order to notify the class
     * implementing this interface that a monitored dispute-related [BlockchainTransaction] has failed (either has been
     * confirmed and failed, or has been dropped.)
     *
     * @param transaction The [BlockchainTransaction] wrapping the on-chain transaction that has failed.
     * @param exception A [BlockchainTransactionException] describing why the on-chain transaction has failed.
     */
    suspend fun handleFailedTransaction(transaction: BlockchainTransaction, exception: BlockchainTransactionException)
}