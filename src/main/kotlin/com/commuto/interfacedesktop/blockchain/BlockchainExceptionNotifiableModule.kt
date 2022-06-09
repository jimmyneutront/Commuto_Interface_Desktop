package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.ui.ExceptionViewModel
import dagger.Binds
import dagger.Module

@Module
interface BlockchainExceptionNotifiableModule {
    @Binds
    fun bindBlockchainExceptionNotifiable(impl: ExceptionViewModel): BlockchainExceptionNotifiable
}