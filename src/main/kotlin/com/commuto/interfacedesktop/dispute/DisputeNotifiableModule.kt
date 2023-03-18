package com.commuto.interfacedesktop.dispute

import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object implementing
 * [DisputeNotifiable]
 */
@Module
interface DisputeNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [DisputeService] into objects that depend on an
     * object implementing [DisputeNotifiable].
     *
     * @param impl The type of object that will be injected into objects that depend on an object implementing
     * [DisputeNotifiable].
     */
    @Binds
    fun bindDisputeNotifiable(impl: DisputeService): DisputeNotifiable
}