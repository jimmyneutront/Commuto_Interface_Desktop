package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.CommutoSwap
import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of offer-related blockchain
 * events by [com.commuto.interfacedesktop.blockchain.BlockchainService].
 */
@Singleton
interface OfferNotifiable {
    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in
     * order to notify the class implementing this interface of a
     * [CommutoSwap.OfferOpenedEventResponse].
     *
     * @param offerOpenedEventResponse The [CommutoSwap.OfferOpenedEventResponse] of which the class
     * implementing this interface is being notified and should handle in the implementation of this
     * method.
     */
    fun handleOfferOpenedEvent(offerOpenedEventResponse: CommutoSwap.OfferOpenedEventResponse)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in
     * order to notify the class implementing this interface of a
     * [CommutoSwap.OfferCanceledEventResponse].
     *
     * @param offerCanceledEventResponse The [CommutoSwap.OfferCanceledEventResponse] of which the
     * class implementing this interface is being notified and should handle in the implementation
     * of this method.
     */
    fun handleOfferCanceledEvent(offerCanceledEventResponse: CommutoSwap.OfferCanceledEventResponse)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in
     * order to notify the class implementing this interface of a
     * [CommutoSwap.OfferTakenEventResponse].
     *
     * @param offerTakenEventResponse The [CommutoSwap.OfferTakenEventResponse] of which the class
     * implementing this interface is being notified and should handle in the implementation of this
     * method.
     */
    fun handleOfferTakenEvent(offerTakenEventResponse: CommutoSwap.OfferTakenEventResponse)
}