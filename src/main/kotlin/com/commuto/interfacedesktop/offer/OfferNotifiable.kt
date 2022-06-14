package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.CommutoSwap
import javax.inject.Singleton

@Singleton
interface OfferNotifiable {
    fun handleOfferOpenedEvent(offerOpenedEventResponse: CommutoSwap.OfferOpenedEventResponse)
    fun handleOfferCanceledEvent(offerCanceledEventResponse: CommutoSwap.OfferCanceledEventResponse)
    fun handleOfferTakenEvent(offerTakenEventResponse: CommutoSwap.OfferTakenEventResponse)
}