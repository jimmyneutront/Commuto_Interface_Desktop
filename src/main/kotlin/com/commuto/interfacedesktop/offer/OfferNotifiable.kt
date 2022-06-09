package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.CommutoSwap
import javax.inject.Singleton

@Singleton
interface OfferNotifiable {
    fun handleOfferOpenedEvent(offerEventResponse: CommutoSwap.OfferOpenedEventResponse)
    fun handleOfferTakenEvent(offerTakenEventResponse: CommutoSwap.OfferTakenEventResponse)
}