package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.CommutoSwap
import com.commuto.interfacedesktop.ui.OffersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Offer Service. It is responsible for processing and organizing offer-related data that
 * it receives from [com.commuto.interfacedesktop.blockchain.BlockchainService] and
 * [com.commuto.interfacedesktop.p2p.P2PService] in order to maintain an accurate list of all
 * open [Offer]s in [OffersViewModel].
 *
 * @property offersTruthSource The [OffersViewModel] in which this is responsible for maintaining an accurate list of
 * all open offers. If this is not yet initialized, event handling methods will throw the corresponding error.
 */
@Singleton
class OfferService @Inject constructor(): OfferNotifiable {

    private lateinit var offersTruthSource: OffersViewModel

    /**
     * Used to set the [offersTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [offersTruthSource] property, which cannot be null.
     */
    fun setOffersTruthSource(newTruthSource: OffersViewModel) {
        check(!::offersTruthSource.isInitialized) {
            "offersTruthSource is already initialized"
        }
        offersTruthSource = newTruthSource
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferOpenedEventResponse]. Once notified,
     * [OfferService] gets the ID of the new offer from [CommutoSwap.OfferOpenedEventResponse],
     * creates a new [Offer] using the data stored in event, and then adds this new [Offer] to
     * [offersTruthSource].
     *
     * @param event The [CommutoSwap.OfferOpenedEventResponse] of which
     * [OfferService] is being notified.
     */
    override suspend fun handleOfferOpenedEvent(event: CommutoSwap.OfferOpenedEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        withContext(Dispatchers.Main) {
            offersTruthSource.offers.add(
                Offer(id = offerId, direction = "Buy", price = "1.004", pair = "USD/USDT")
            )
        }
    }

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferCanceledEventResponse]. Once notified,
     * [OfferService] gets the ID of the now-canceled offer from
     * [CommutoSwap.OfferCanceledEventResponse] and removes the [Offer] with the specified ID from
     * [offersTruthSource].
     *
     * @param event The [CommutoSwap.OfferCanceledEventResponse] of which
     * [OfferService] is being notified.
     */
    override suspend fun handleOfferCanceledEvent(event: CommutoSwap.OfferCanceledEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        withContext(Dispatchers.Main) {
            offersTruthSource.offers.removeIf { it.id == offerId }
        }
    }

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to
     * notify [OfferService] of an [CommutoSwap.OfferTakenEventResponse]. Once notified,
     * [OfferService] gets the ID of the now-taken offer from
     * [CommutoSwap.OfferTakenEventResponse] and removes the [Offer] with the specified ID from
     * [offersTruthSource].
     *
     * @param event The [CommutoSwap.OfferTakenEventResponse] of which
     * [OfferService] is being notified.
     */
    override suspend fun handleOfferTakenEvent(event: CommutoSwap.OfferTakenEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        withContext(Dispatchers.Main) {
            offersTruthSource.offers.removeIf { it.id == offerId }
        }
    }
}