package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiableModule
import com.commuto.interfacedesktop.offer.OfferNotifiableModule
import dagger.Component
import javax.inject.Singleton

// Note: Dagger causes building to fail if the last element in the modules list has a trailing comma
@Singleton
@Component(modules = [OfferNotifiableModule::class,
    BlockchainExceptionNotifiableModule::class
])
interface CommutoApplicationFactory {
    fun commutoApplicationFactory(): CommutoApplication
}