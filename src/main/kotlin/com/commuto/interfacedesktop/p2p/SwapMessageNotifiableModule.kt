package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.swap.SwapService
import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object implementing
 * [SwapMessageNotifiable]
 */
@Module
interface SwapMessageNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [SwapService] into objects that depend on an object
     * implementing [SwapMessageNotifiable].
     *
     * @param impl The type of object that will be injected into objects that depend on an object implementing
     * [SwapMessageNotifiable].
     */
    @Binds
    fun bindSwapMessageNotifiable(impl: SwapService): SwapMessageNotifiable
}