package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiableModule
import com.commuto.interfacedesktop.offer.OfferNotifiableModule
import dagger.Component
import javax.inject.Singleton

// Note: Dagger causes building to fail if the last element in the modules list has a trailing comma
/**
 * A [CommutoApplication] factory, required by Dagger in order to automatically generate the
 * [DaggerCommutoApplicationFactory] class.
 */
@Singleton
@Component(modules = [OfferNotifiableModule::class,
    BlockchainExceptionNotifiableModule::class
])
interface CommutoApplicationFactory {
    /**
     * Tells Dagger how to generate [CommutoApplicationFactory]'s [commutoApplicationFactory] method.
     */
    fun commutoApplicationFactory(): CommutoApplication
}