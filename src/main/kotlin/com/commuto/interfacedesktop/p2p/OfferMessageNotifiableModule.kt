package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.offer.OfferService
import dagger.Binds
import dagger.Module

/**
 * A Dagger [Module] that tells Dagger what to inject into objects that depend on an object
 * implementing [OfferMessageNotifiable]
 */
@Module
interface OfferMessageNotifiableModule {
    /**
     * A Dagger Binding that tells Dagger to inject an instance of [OfferService] into objects
     * that depend on an object implementing [OfferMessageNotifiable].
     *
     * @param impl The type of object that will be injected into objects that depend on an object
     * implementing [OfferMessageNotifiable].
     */
    @Binds
    fun bindOfferMessageNotifiable(impl: OfferService): OfferMessageNotifiable
}