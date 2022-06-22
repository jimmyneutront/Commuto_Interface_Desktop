package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Exception View Model, used to display a visual notification to the user when unexpected
 * exceptions occur.
 */
@Singleton
class ExceptionViewModel @Inject constructor(): BlockchainExceptionNotifiable {
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
}