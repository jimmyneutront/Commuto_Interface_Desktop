package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiable
import com.commuto.interfacedesktop.p2p.P2PExceptionNotifiable
import com.commuto.interfacedesktop.p2p.P2PService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Exception View Model, used to display a visual notification to the user when unexpected
 * exceptions occur.
 */
@Singleton
class ExceptionViewModel @Inject constructor(): BlockchainExceptionNotifiable, P2PExceptionNotifiable {
    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in order to notify of an
     * [Exception] encountered by [com.commuto.interfacedesktop.blockchain.BlockchainService].
     *
     * @param exception The [Exception] encountered by [com.commuto.interfacedesktop.blockchain.BlockchainService] about
     * which this method should notify the user.
     */
    override fun handleBlockchainException(exception: Exception) {
        throw exception
    }

    /**
     * The method called by [P2PService] in order to notify of an encountered [Exception].
     *
     * @param exception The [Exception] encountered by [P2PService] that this method should handle
     */
    override fun handleP2PException(exception: Exception) {
        throw exception
    }
}