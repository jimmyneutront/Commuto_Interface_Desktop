package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.CommutoSwap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfferService @Inject constructor(): OfferNotifiable {
    var offers = mutableStateListOf<Offer>() //Offer.manySampleOffers
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun handleOfferOpenedEvent(offerOpenedEventResponse: CommutoSwap.OfferOpenedEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerOpenedEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.add(Offer(id = offerId, direction = "Buy", price = "1.004", pair = "USD/USDT"))
    }

    override fun handleOfferCanceledEvent(offerCanceledEventResponse: CommutoSwap.OfferCanceledEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerCanceledEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.removeIf { it.id == offerId }
    }

    override fun handleOfferTakenEvent(offerTakenEventResponse: CommutoSwap.OfferTakenEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerTakenEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.removeIf { it.id == offerId }
    }
}