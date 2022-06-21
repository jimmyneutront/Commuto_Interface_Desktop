package com.commuto.interfacedesktop.blockchain

import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of [Exception]s encountered by
 * [BlockchainService].
 */
@Singleton
interface BlockchainExceptionNotifiable {
    /**
     * The method called by [BlockchainService] in order to notify the class implementing this
     * interface of an [Exception] encountered by [BlockchainService].
     *
     * @param exception The [Exception] encountered by [BlockchainService] of which the class
     * implementing this interface is being notified and should handle in the implementation of this
     * method.
     */
    fun handleBlockchainException(exception: Exception)
}