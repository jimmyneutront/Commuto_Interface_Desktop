package com.commuto.interfacemobile.android.p2p

import com.commuto.interfacedesktop.p2p.P2PExceptionNotifiable
import com.commuto.interfacedesktop.ui.ExceptionViewModel
import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object implementing
 * [P2PExceptionNotifiable]
 */
@Module
interface P2PExceptionNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [ExceptionViewModel] into objects
     * that depend on an object implementing [P2PExceptionNotifiableModule].
     *
     * @param impl The type of object that will be injected into objects that depend on an object
     * implementing [P2PExceptionNotifiableModule].
     */
    @Binds
    fun bindP2PExceptionNotifiable(impl: ExceptionViewModel): P2PExceptionNotifiable
}