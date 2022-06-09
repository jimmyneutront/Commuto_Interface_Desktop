package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionViewModel @Inject constructor(): BlockchainExceptionNotifiable {
    override fun handleBlockchainException(exception: Exception) {
        throw exception
    }
}