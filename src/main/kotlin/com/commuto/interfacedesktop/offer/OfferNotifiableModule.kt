package com.commuto.interfacedesktop.offer

import dagger.Binds
import dagger.Module

@Module
interface OfferNotifiableModule {
    @Binds
    fun bindOfferNotifiable(impl: OfferService): OfferNotifiable
}