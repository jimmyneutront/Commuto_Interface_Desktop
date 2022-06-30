package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
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
 * @property offerOpenedEventsRepository A repository containing [CommutoSwap.OfferOpenedEventResponse]s for offers that
 * are open and for which complete offer information has not yet been retrieved.
 */
@Singleton
class OfferService (
    private val databaseService: DatabaseService,
    private val offerOpenedEventsRepository: BlockchainEventRepository<CommutoSwap.OfferOpenedEventResponse>
): OfferNotifiable {

    @Inject constructor(databaseService: DatabaseService): this(databaseService, BlockchainEventRepository())

    private lateinit var offersTruthSource: OffersViewModel

    private lateinit var blockchainService: BlockchainService

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

    /**
     * Used to set the [blockchainService] property. This can only be called once.
     *
     * @param newBlockchainService The new value of the [blockchainService]
     */
    fun setBlockchainService(newBlockchainService: BlockchainService) {
        check(!::blockchainService.isInitialized) {
            "blockchainService is already initialized"
        }
        blockchainService = newBlockchainService
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] to notify [OfferService]
     * of an [CommutoSwap.OfferOpenedEventResponse]. Once notified, [OfferService] persistently stores [event], saves it
     * in [offerOpenedEventsRepository], gets all on-chain offer data by calling [blockchainService]'s
     * [BlockchainService.getOfferAsync] method, creates a new [Offer] with the results, persistently stores the new
     * offer, removes [event] from persistent storage, and then adds the new [Offer] to [offersTruthSource] on the main
     * coroutine dispatcher.
     *
     * @param event The [CommutoSwap.OfferOpenedEventResponse] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferOpenedEvent(
        event: CommutoSwap.OfferOpenedEventResponse
    ) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        val encoder = Base64.getEncoder()
        databaseService.storeOfferOpenedEvent(
            id = encoder.encodeToString(event.offerID),
            interfaceId = encoder.encodeToString(event.interfaceId)
        )
        offerOpenedEventsRepository.append(event)
        val onChainOffer = blockchainService.getOfferAsync(offerId).await()
        val offer = Offer(
            id = offerId,
            direction = "Buy",
            price = "1.004",
            pair = "USD/USDT",
            isCreated = onChainOffer.isCreated,
            isTaken = onChainOffer.isTaken,
            maker = onChainOffer.maker,
            interfaceId = onChainOffer.interfaceId,
            stablecoin = onChainOffer.stablecoin,
            amountLowerBound = onChainOffer.amountLowerBound,
            amountUpperBound = onChainOffer.amountUpperBound,
            securityDepositAmount = onChainOffer.securityDepositAmount,
            serviceFeeRate = onChainOffer.serviceFeeRate,
            onChainDirection = onChainOffer.direction,
            onChainPrice = onChainOffer.price,
            settlementMethods = onChainOffer.settlementMethods,
            protocolVersion = onChainOffer.protocolVersion
        )
        val isCreated = if (onChainOffer.isCreated) 1L else 0L
        val isTaken = if (onChainOffer.isTaken) 1L else 0L
        val offerForDatabase = DatabaseOffer(
            offerId = encoder.encodeToString(offerIdByteBuffer.array()),
            isCreated = isCreated,
            isTaken = isTaken,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            onChainPrice = encoder.encodeToString(offer.onChainPrice),
            protocolVersion = offer.protocolVersion.toString()
        )
        databaseService.storeOffer(offerForDatabase)
        val settlementMethodStrings = offer.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerForDatabase.offerId, settlementMethodStrings)
        databaseService.deleteOfferOpenedEvents(encoder.encodeToString(event.offerID))
        offerOpenedEventsRepository.remove(event)
        withContext(Dispatchers.Main) {
            offersTruthSource.addOffer(offer)
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