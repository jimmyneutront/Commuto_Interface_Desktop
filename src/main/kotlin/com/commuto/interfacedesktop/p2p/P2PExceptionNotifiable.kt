package com.commuto.interfacedesktop.p2p

import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of [Exception]s encountered by
 * [P2PService].
 */
@Singleton
interface P2PExceptionNotifiable {
    /**
     * The method called by [P2PService] in order to notify the class implementing this interface of
     * an [Exception] encountered by [P2PService].
     *
     * @param exception The [Exception] encountered by [P2PService] of which the class implementing
     * this interface is being notified and should handle in the implementation of this method.
     */
    fun handleP2PException(exception: Exception)
}