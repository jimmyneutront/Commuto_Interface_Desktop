package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.OfferCanceledEvent
import com.commuto.interfacedesktop.blockchain.events.commutoswap.OfferEditedEvent
import com.commuto.interfacedesktop.blockchain.events.commutoswap.OfferOpenedEvent
import com.commuto.interfacedesktop.blockchain.events.commutoswap.OfferTakenEvent
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
 * [com.commuto.interfacedesktop.blockchain.BlockchainService] and [com.commuto.interfacedesktop.p2p.P2PService] in
 * order to maintain an accurate list of all open [Offer]s in [OffersViewModel].
 *
 * @property databaseService The [DatabaseService] used for  persistent storage.
 * @property offerOpenedEventRepository A repository containing [OfferOpenedEvent]s for offers that are open and for
 * which complete offer information has not yet been retrieved.
 * @property offerEditedEventRepository A repository containing [OfferEditedEvent]s for offers that are open and have
 * been edited by their makers, meaning price and payment method information stored in this interface's persistent
 * storage is currently inaccurate.
 * @property offerCanceledEventRepository A repository containing [OfferCanceledEvent]s for offers that have been
 * canceled but haven't yet been removed from persistent storage or [offerTruthSource].
 * @property offerTakenEventRepository A repository containing [OfferTakenEvent]s for offers that have been taken but
 * haven't yet been removed from persistent storage or [offerTruthSource].
 * @property offerTruthSource The [OfferTruthSource] in which this is responsible for maintaining an accurate list of
 * all open offers. If this is not yet initialized, event handling methods will throw the corresponding error.
 */
@Singleton
class OfferService (
    private val databaseService: DatabaseService,
    private val offerOpenedEventRepository: BlockchainEventRepository<OfferOpenedEvent>,
    private val offerEditedEventRepository: BlockchainEventRepository<OfferEditedEvent>,
    private val offerCanceledEventRepository: BlockchainEventRepository<OfferCanceledEvent>,
    private val offerTakenEventRepository: BlockchainEventRepository<OfferTakenEvent>
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
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferOpenedEvent]. Once notified,
     * [OfferService] saves [event] in offerOpenedEventRepository], gets all on-chain offer data by calling
     * [blockchainService]'s [BlockchainService.getOffer] method, verifies that the chain ID of the event and the offer
     * data match, creates a new [Offer] and list of settlement methods with the results, persistently stores the new
     * offer and its settlement methods, removes [event] from [offerOpenedEventRepository], and then adds the new
     * [Offer] to [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [OfferOpenedEvent] of which [OfferService] is being notified.
     *
     * @throws [IllegalStateException] if the chain ID of [event] doesn't match the chain ID of the offer obtained
     * from [BlockchainService.getOffer] when called with [OfferOpenedEvent.offerID].
     */
    @Throws(IllegalStateException::class)
    override suspend fun handleOfferOpenedEvent(
        event: OfferOpenedEvent
    ) {
        val encoder = Base64.getEncoder()
        offerOpenedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException("Chain ID of OfferOpenedEvent did not match chain ID of OfferStruct in " +
                    "handleOfferOpenedEvent call. OfferOpenedEvent.chainID: ${event.chainID}, " +
                    "OfferStruct.chainID: ${offerStruct.chainID}")
        }
        val offer = Offer(
            isCreated = offerStruct.isCreated,
            isTaken = offerStruct.isTaken,
            id = event.offerID,
            maker = offerStruct.maker,
            interfaceId = offerStruct.interfaceID,
            stablecoin = offerStruct.stablecoin,
            amountLowerBound = offerStruct.amountLowerBound,
            amountUpperBound = offerStruct.amountUpperBound,
            securityDepositAmount = offerStruct.securityDepositAmount,
            serviceFeeRate = offerStruct.serviceFeeRate,
            onChainDirection = offerStruct.direction,
            settlementMethods = offerStruct.settlementMethods,
            protocolVersion = offerStruct.protocolVersion,
            chainID = offerStruct.chainID
        )
        val isCreated = if (offerStruct.isCreated) 1L else 0L
        val isTaken = if (offerStruct.isTaken) 1L else 0L
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(offer.id.mostSignificantBits)
        offerIDByteBuffer.putLong(offer.id.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerForDatabase = DatabaseOffer(
            isCreated = isCreated,
            isTaken = isTaken,
            offerId = encoder.encodeToString(offerIDByteArray),
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString()
        )
        databaseService.storeOffer(offerForDatabase)
        val settlementMethodStrings = offer.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerForDatabase.offerId, offerForDatabase.chainID,
            settlementMethodStrings)
        offerOpenedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.addOffer(offer)
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [OfferEditedEvent]. Once notified,
     * [OfferService] saves [event] in [offerEditedEventRepository], gets updated on-chain offer data by calling
     * [blockchainService]'s [BlockchainService.getOffer] method, verifies that the chain ID of the event and the offer
     * data match, creates an updated [Offer] and with the results, updates the settlement methods of the corresponding
     * persistently stored offer, removes [event] from [offerEditedEventRepository], and then adds the updated [Offer]
     * to [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [OfferEditedEvent] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
        val encoder = Base64.getEncoder()
        offerEditedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException("Chain ID of OfferEditedEvent did not match chain ID of OfferStruct in " +
                    "handleOfferEditedEvent call. OfferEditedEvent.chainID: ${event.chainID}, " +
                    "OfferStruct.chainID: ${offerStruct.chainID}")
        }
        val offer = Offer(
            isCreated = offerStruct.isCreated,
            isTaken = offerStruct.isTaken,
            id = event.offerID,
            maker = offerStruct.maker,
            interfaceId = offerStruct.interfaceID,
            stablecoin = offerStruct.stablecoin,
            amountLowerBound = offerStruct.amountLowerBound,
            amountUpperBound = offerStruct.amountUpperBound,
            securityDepositAmount = offerStruct.securityDepositAmount,
            serviceFeeRate = offerStruct.serviceFeeRate,
            onChainDirection = offerStruct.direction,
            settlementMethods = offerStruct.settlementMethods,
            protocolVersion = offerStruct.protocolVersion,
            chainID = offerStruct.chainID
        )
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(offer.id.mostSignificantBits)
        offerIDByteBuffer.putLong(offer.id.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = encoder.encodeToString(offerIDByteArray)
        val settlementMethodStrings = offer.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerIdString, offer.chainID.toString(), settlementMethodStrings)
        offerEditedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.removeOffer(offer.id)
            offerTruthSource.addOffer(offer)
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferCanceledEvent]. Once notified,
     * [OfferService] saves [event] in [offerCanceledEventRepository], removes the corresponding [Offer] and its
     * settlement methods from persistent storage, removes [event] from [offerCanceledEventRepository], and then checks
     * that the chain ID of the event matches the chain ID of the [Offer] mapped to the offer ID specified in [event] in
     * the [OfferTruthSource.offers] list on the main coroutine dispatcher. If they do not match, this returns. If they
     * do match, then this synchronously removes the [Offer] from said list on the main thread.
     *
     * @param event The [OfferCanceledEvent] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferCanceledEvent(
        event: OfferCanceledEvent
    ) {
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = Base64.getEncoder().encodeToString(offerIDByteArray)
        offerCanceledEventRepository.append(event)
        databaseService.deleteOffers(offerIdString, event.chainID.toString())
        databaseService.deleteSettlementMethods(offerIdString, event.chainID.toString())
        offerCanceledEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            if (offerTruthSource.offers[event.offerID]?.chainID == event.chainID) {
                offerTruthSource.removeOffer(event.offerID)
            }
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [OfferTakenEvent]. Once notified,
     * [OfferService] saves [event] in [offerTakenEventRepository], removes the corresponding [Offer] and its settlement
     * methods from persistent storage, removes [event] from [offerTakenEventRepository], and then checks that the chain
     * ID of the event matches the chain ID of the [Offer] mapped to the offer ID specified in [event] in the
     * [OfferTruthSource.offers] list on the main coroutine dispatcher. If they do not match, this returns. If they do
     * match, then this synchronously removes the [Offer] from said list on the main thread.
     *
     * @param event The [OfferTakenEvent] of which [OfferService] is being notified.
     */
    override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = Base64.getEncoder().encodeToString(offerIDByteArray)
        offerTakenEventRepository.append(event)
        databaseService.deleteOffers(offerIdString, event.chainID.toString())
        databaseService.deleteSettlementMethods(offerIdString, event.chainID.toString())
        offerTakenEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            if (offerTruthSource.offers[event.offerID]?.chainID == event.chainID) {
                offerTruthSource.removeOffer(event.offerID)
            }
        }
    }
}