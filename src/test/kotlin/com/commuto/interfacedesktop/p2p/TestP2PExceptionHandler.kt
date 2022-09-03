package com.commuto.interfacedesktop.p2p

/**
 * A basic [P2PExceptionNotifiable] implementation for testing.
 */
class TestP2PExceptionHandler : P2PExceptionNotifiable {
    @Throws
    override fun handleP2PException(exception: Exception) {
        throw exception
    }
}