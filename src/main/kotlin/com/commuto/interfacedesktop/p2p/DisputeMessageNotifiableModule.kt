package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.dispute.DisputeService
import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object implementing
 * [DisputeMessageNotifiable]
 */
@Module
interface DisputeMessageNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [DisputeService] into objects that depend on an
     * object implementing [DisputeMessageNotifiable].
     *
     * @param impl The type of object that will be injected into objects that depend on an object implementing
     * [DisputeMessageNotifiable].
     */
    @Binds
    fun bindDisputeMessageNotifiable(impl: DisputeService): DisputeMessageNotifiable
}