package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiableModule
import com.commuto.interfacedesktop.dispute.DisputeNotifiableModule
import com.commuto.interfacedesktop.offer.OfferNotifiableModule
import com.commuto.interfacedesktop.p2p.DisputeMessageNotifiableModule
import com.commuto.interfacedesktop.p2p.OfferMessageNotifiableModule
import com.commuto.interfacedesktop.p2p.P2PExceptionNotifiableModule
import com.commuto.interfacedesktop.p2p.SwapMessageNotifiableModule
import com.commuto.interfacedesktop.swap.SwapNotifiableModule
import dagger.Component
import javax.inject.Singleton

// Note: Dagger causes building to fail if the last element in the modules list has a trailing comma
/**
 * A [CommutoApplication] factory, required by Dagger in order to automatically generate the
 * [DaggerCommutoApplicationFactory] class.
 */
@Singleton
@Component(modules = [
    OfferNotifiableModule::class,
    OfferMessageNotifiableModule::class,
    SwapNotifiableModule::class,
    SwapMessageNotifiableModule::class,
    DisputeNotifiableModule::class,
    DisputeMessageNotifiableModule::class,
    BlockchainExceptionNotifiableModule::class,
    P2PExceptionNotifiableModule::class
])
interface CommutoApplicationFactory {
    /**
     * Tells Dagger how to generate [CommutoApplicationFactory]'s [commutoApplicationFactory] method.
     */
    fun commutoApplicationFactory(): CommutoApplication
}