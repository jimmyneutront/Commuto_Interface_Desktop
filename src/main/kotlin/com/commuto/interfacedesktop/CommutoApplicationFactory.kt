package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.offer.OfferNotifiableModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [OfferNotifiableModule::class])
interface CommutoApplicationFactory {
    fun commutoApplicationFactory(): CommutoApplication
}