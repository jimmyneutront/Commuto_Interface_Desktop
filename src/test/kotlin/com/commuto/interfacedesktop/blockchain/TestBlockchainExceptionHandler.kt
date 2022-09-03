package com.commuto.interfacedesktop.blockchain

/**
 * A basic [BlockchainExceptionNotifiable] implementation for testing.
 */
class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
    var gotError = false
    override fun handleBlockchainException(exception: Exception) {
        gotError = true
    }
}