package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.ui.ExceptionViewModel
import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object
 * implementing [BlockchainExceptionNotifiable]
 */
@Module
interface BlockchainExceptionNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [ExceptionViewModel] into objects
     * that depend on an object implementing [BlockchainExceptionNotifiable].
     *
     * @param impl The type of object that will be injected into objects that depend on an object
     * implementing [BlockchainExceptionNotifiable].
     */
    @Binds
    fun bindBlockchainExceptionNotifiable(impl: ExceptionViewModel): BlockchainExceptionNotifiable
}