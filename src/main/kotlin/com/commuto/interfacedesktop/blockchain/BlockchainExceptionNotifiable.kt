package com.commuto.interfacedesktop.blockchain

import javax.inject.Singleton

@Singleton
interface BlockchainExceptionNotifiable {
    fun handleBlockchainException(exception: Exception)
}