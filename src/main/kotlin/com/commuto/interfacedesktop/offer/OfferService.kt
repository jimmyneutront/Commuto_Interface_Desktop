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
 * The main Offer Service. It is responsible for processing and organizing offer-related data that it receives from
 * [BlockchainService] and [com.commuto.interfacedesktop.p2p.P2PService] in order to maintain an accurate list of all
 * open [Offer]s in [OffersViewModel].
 *
 * @property offerTruthSource The [OffersViewModel] in which this is responsible for maintaining an accurate list of
 * all open offers. If this is not yet initialized, event handling methods will throw the corresponding error.
 * @property offerOpenedEventRepository A repository containing [CommutoSwap.OfferOpenedEventResponse]s for offers that
 * are open and for which complete offer information has not yet been retrieved.
 * @property offerEditedEventRepository A repository containing [CommutoSwap.OfferEditedEventResponse]s for offers that
 * are open and for which stored price and payment method information is currently inaccurate.
 * @property offerCanceledEventRepository A repository containing [CommutoSwap.OfferCanceledEventResponse]s for offers
 * that have been canceled but haven't yet been removed from persistent storage or [offerTruthSource].
 * @property offerTakenEventRepository A repository containing [CommutoSwap.OfferTakenEventResponse]s for offers
 * that have been taken but haven't yet been removed from persistent storage or [offerTruthSource].
 */
@Singleton
class OfferService (
    private val databaseService: DatabaseService,
    private val offerOpenedEventRepository: BlockchainEventRepository<CommutoSwap.OfferOpenedEventResponse>,
    private val offerEditedEventRepository: BlockchainEventRepository<CommutoSwap.OfferEditedEventResponse>,
    private val offerCanceledEventRepository: BlockchainEventRepository<CommutoSwap.OfferCanceledEventResponse>,
    private val offerTakenEventRepository: BlockchainEventRepository<CommutoSwap.OfferTakenEventResponse>
): OfferNotifiable {

    @Inject constructor(databaseService: DatabaseService):
            this(
                databaseService,
                BlockchainEventRepository(),
                BlockchainEventRepository(),
                BlockchainEventRepository(),
                BlockchainEventRepository(),
            )

    private lateinit var offerTruthSource: OfferTruthSource

    private lateinit var blockchainService: BlockchainService

    /**
     * Used to set the [offerTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [offerTruthSource] property, which cannot be null.
     */
    fun setOfferTruthSource(newTruthSource: OfferTruthSource) {
        check(!::offerTruthSource.isInitialized) {
            "offersTruthSource is already initialized"
        }
        offerTruthSource = newTruthSource
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
     * The method called by [BlockchainService] to notify [OfferService] of an [CommutoSwap.OfferOpenedEventResponse].
     * Once notified, [OfferService] saves [event] in [offerOpenedEventRepository], gets all on-chain offer data by
     * calling [blockchainService]'s [BlockchainService.getOfferAsync] method, creates a new [Offer] and list of
     * settlement methods with the results, persistently stores the new offer and its settlement methods, removes
     * [event] from [offerOpenedEventRepository], and then adds the new [Offer] to [offerTruthSource] on the main
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
        offerOpenedEventRepository.append(event)
        val onChainOffer = blockchainService.getOfferAsync(offerId).await()
        val offer = Offer(
            isCreated = onChainOffer.isCreated,
            isTaken = onChainOffer.isTaken,
            id = offerId,
            maker = onChainOffer.maker,
            interfaceId = onChainOffer.interfaceId,
            stablecoin = onChainOffer.stablecoin,
            amountLowerBound = onChainOffer.amountLowerBound,
            amountUpperBound = onChainOffer.amountUpperBound,
            securityDepositAmount = onChainOffer.securityDepositAmount,
            serviceFeeRate = onChainOffer.serviceFeeRate,
            onChainDirection = onChainOffer.direction,
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
            protocolVersion = offer.protocolVersion.toString()
        )
        databaseService.storeOffer(offerForDatabase)
        val settlementMethodStrings = offer.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerForDatabase.offerId, settlementMethodStrings)
        offerOpenedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.addOffer(offer)
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService]
     * of an [CommutoSwap.OfferEditedEventResponse]. Once notified, [OfferService] saves [event] in
     * [offerEditedEventRepository], gets updated on-chain offer data by calling [blockchainService]'s
     * [BlockchainService.getOfferAsync] method, creates an updated [Offer] and with the results, updates the settlement
     * methods of the corresponding persistently stored offer, removes [event] from [offerEditedEventRepository], and
     * then adds the updated [Offer] to [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [CommutoSwap.OfferEditedEventResponse] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferEditedEvent(event: CommutoSwap.OfferEditedEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        val encoder = Base64.getEncoder()
        offerEditedEventRepository.append(event)
        val onChainOffer = blockchainService.getOfferAsync(offerId).await()
        val offer = Offer(
            isCreated = onChainOffer.isCreated,
            isTaken = onChainOffer.isTaken,
            id = offerId,
            maker = onChainOffer.maker,
            interfaceId = onChainOffer.interfaceId,
            stablecoin = onChainOffer.stablecoin,
            amountLowerBound = onChainOffer.amountLowerBound,
            amountUpperBound = onChainOffer.amountUpperBound,
            securityDepositAmount = onChainOffer.securityDepositAmount,
            serviceFeeRate = onChainOffer.serviceFeeRate,
            onChainDirection = onChainOffer.direction,
            settlementMethods = onChainOffer.settlementMethods,
            protocolVersion = onChainOffer.protocolVersion
        )
        val offerIdString = encoder.encodeToString(offerIdByteBuffer.array())
        val settlementMethodStrings = offer.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerIdString, settlementMethodStrings)
        offerEditedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.removeOffer(offerId)
            offerTruthSource.addOffer(offer)
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [CommutoSwap.OfferCanceledEventResponse].
     * Once notified, [OfferService] saves [event] in [offerCanceledEventRepository], removes the corresponding [Offer]
     * and its settlement methods from persistent storage, removes [event] from [offerCanceledEventRepository], and then
     * removes the corresponding [Offer] from [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [CommutoSwap.OfferCanceledEventResponse] of which
     * [OfferService] is being notified.
     */
    override suspend fun handleOfferCanceledEvent(event: CommutoSwap.OfferCanceledEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        val offerIdString = Base64.getEncoder().encodeToString(event.offerID)
        offerCanceledEventRepository.append(event)
        databaseService.deleteOffers(offerIdString)
        databaseService.deleteSettlementMethods(offerIdString)
        offerCanceledEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.removeOffer(offerId)
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [CommutoSwap.OfferTakenEventResponse].
     * Once notified, [OfferService] saves [event] in [offerTakenEventRepository], removes the corresponding [Offer] and
     * its settlement methods from persistent storage, removes [event] from [offerTakenEventRepository], and then
     * removes the corresponding [Offer] from [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [CommutoSwap.OfferTakenEventResponse] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferTakenEvent(event: CommutoSwap.OfferTakenEventResponse) {
        val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
        val mostSigBits = offerIdByteBuffer.long
        val leastSigBits = offerIdByteBuffer.long
        val offerId = UUID(mostSigBits, leastSigBits)
        val offerIdString = Base64.getEncoder().encodeToString(event.offerID)
        offerTakenEventRepository.append(event)
        databaseService.deleteOffers(offerIdString)
        databaseService.deleteSettlementMethods(offerIdString)
        offerTakenEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.removeOffer(offerId)
        }
    }
}