package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.CommutoSwap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Offer Service. It is responsible for processing and organizing offer-related data that
 * it receives from [com.commuto.interfacedesktop.blockchain.BlockchainService] and
 * [com.commuto.interfacedesktop.p2p.P2PService] in order to maintain an accurate list of all
 * open [Offer]s in [com.commuto.interfacedesktop.ui.OffersViewModel].
 *
 * @property offers A pointer to the list of all open offers stored in an
 * [com.commuto.interfacedesktop.ui.OffersViewModel] instance.
 */
@Singleton
class OfferService @Inject constructor(): OfferNotifiable {
    var offers = mutableStateListOf<Offer>() //Offer.manySampleOffers
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferOpenedEventResponse]. Once notified,
     * [OfferService] gets the ID of the new offer from [CommutoSwap.OfferOpenedEventResponse],
     * creates a new [Offer] using the data stored in event, and then adds this new [Offer] to
     * [offers].
     *
     * @param offerOpenedEventResponse The [CommutoSwap.OfferOpenedEventResponse] of which
     * [OfferService] is being notified.
     */
    override fun handleOfferOpenedEvent(offerOpenedEventResponse: CommutoSwap.OfferOpenedEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerOpenedEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.add(Offer(id = offerId, direction = "Buy", price = "1.004", pair = "USD/USDT"))
    }

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferCanceledEventResponse]. Once notified,
     * [OfferService] gets the ID of the now-canceled offer from
     * [CommutoSwap.OfferCanceledEventResponse] and removes the [Offer] with the specified ID from
     * [offers].
     *
     * @param offerCanceledEventResponse The [CommutoSwap.OfferCanceledEventResponse] of which
     * [OfferService] is being notified.
     */
    override fun handleOfferCanceledEvent(offerCanceledEventResponse: CommutoSwap.OfferCanceledEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerCanceledEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.removeIf { it.id == offerId }
    }

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferTakenEventResponse]. Once notified,
     * [OfferService] gets the ID of the now-taken offer from
     * [CommutoSwap.OfferTakenEventResponse] and removes the [Offer] with the specified ID from
     * [offers].
     *
     * @param offerTakenEventResponse The [CommutoSwap.OfferTakenEventResponse] of which
     * [OfferService] is being notified.
     */
    override fun handleOfferTakenEvent(offerTakenEventResponse: CommutoSwap.OfferTakenEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(offerTakenEventResponse.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        offers.removeIf { it.id == offerId }
    }
}