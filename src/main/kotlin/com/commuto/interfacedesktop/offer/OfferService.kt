package com.commuto.interfacedesktop.offer

import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.p2p.OfferMessageNotifiable
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.ui.OffersViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.math.BigInteger
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
 * @property keyManagerService The [KeyManagerService] that the [OfferService] will use for managing keys.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property offerOpenedEventRepository A repository containing [OfferOpenedEvent]s for offers that are open and for
 * which complete offer information has not yet been retrieved.
 * @property offerEditedEventRepository A repository containing [OfferEditedEvent]s for offers that are open and have
 * been edited by their makers, meaning price and payment method information stored in this interface's persistent
 * storage is currently inaccurate.
 * @property offerCanceledEventRepository A repository containing [OfferCanceledEvent]s for offers that have been
 * canceled but haven't yet been removed from persistent storage or [offerTruthSource].
 * @property offerTakenEventRepository A repository containing [OfferTakenEvent]s for offers that have been taken but
 * haven't yet been removed from persistent storage or [offerTruthSource].
 * @property serviceFeeRateChangedEventRepository A repository containing [ServiceFeeRateChangedEvent]s
 * @property offerTruthSource The [OfferTruthSource] in which this is responsible for maintaining an accurate list of
 * all open offers. If this is not yet initialized, event handling methods will throw the corresponding error.
 */
@Singleton
class OfferService (
    private val databaseService: DatabaseService,
    private val keyManagerService: KeyManagerService,
    private val offerOpenedEventRepository: BlockchainEventRepository<OfferOpenedEvent>,
    private val offerEditedEventRepository: BlockchainEventRepository<OfferEditedEvent>,
    private val offerCanceledEventRepository: BlockchainEventRepository<OfferCanceledEvent>,
    private val offerTakenEventRepository: BlockchainEventRepository<OfferTakenEvent>,
    private val serviceFeeRateChangedEventRepository: BlockchainEventRepository<ServiceFeeRateChangedEvent>
): OfferNotifiable, OfferMessageNotifiable {

    @Inject constructor(databaseService: DatabaseService, keyManagerService: KeyManagerService): this(
        databaseService,
        keyManagerService,
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository()
    )

    private lateinit var offerTruthSource: OfferTruthSource

    private lateinit var blockchainService: BlockchainService

    private val logger = LoggerFactory.getLogger(javaClass)

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

    /**
     * Returns the result of calling [blockchainService]'s [getServiceFeeRateAsync] method.
     *
     * @return A [Deferred] with a [BigInteger] result, which is the current service fee rate.
     */
    fun getServiceFeeRateAsync(): Deferred<BigInteger> {
        return blockchainService.getServiceFeeRateAsync()
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferOpenedEvent]. Once notified,
     * [OfferService] saves [event] in offerOpenedEventRepository], gets all on-chain offer data by calling
     * [blockchainService]'s [BlockchainService.getOffer] method, verifies that the chain ID of the event and the offer
     * data match, creates a new [Offer] and list of settlement methods with the results, checks if [keyManagerService]
     * has the maker's public key and updates the [Offer.havePublicKey] property accordingly, persistently stores the
     * new offer and its settlement methods, removes [event] from [offerOpenedEventRepository], and then adds the new
     * [Offer] to [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [OfferOpenedEvent] of which [OfferService] is being notified.
     *
     * @throws [IllegalStateException] if no offer is found with the ID specified in [event], or if the chain ID of
     * [event] doesn't match the chain ID of the offer obtained from [BlockchainService.getOffer] when called with
     * [OfferOpenedEvent.offerID].
     */
    @Throws(IllegalStateException::class)
    override suspend fun handleOfferOpenedEvent(
        event: OfferOpenedEvent
    ) {
        logger.info("handleOfferOpenedEvent: handling event for offer ${event.offerID}")
        val encoder = Base64.getEncoder()
        offerOpenedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (offerStruct == null) {
            logger.info("handleOfferOpenedEvent: no on-chain offer was found with ID specified in OfferOpenedEvent " +
                    "in handleOfferOpenedEvent call. OfferOpenedEvent.id: ${event.offerID}")
            return
        }
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException("Chain ID of OfferOpenedEvent did not match chain ID of OfferStruct in " +
                    "handleOfferOpenedEvent call. OfferOpenedEvent.chainID: ${event.chainID}, " +
                    "OfferStruct.chainID: ${offerStruct.chainID}, OfferOpenedEvent.offerID: ${event.offerID}")
        }
        val havePublicKey = (keyManagerService.getPublicKey(offerStruct.interfaceID) != null)
        logger.info("handleOfferOpenedEvent: havePublicKey for offer ${event.offerID}: $havePublicKey")
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
            onChainSettlementMethods = offerStruct.settlementMethods,
            protocolVersion = offerStruct.protocolVersion,
            chainID = offerStruct.chainID,
            havePublicKey = havePublicKey,
        )
        val isCreated = if (offerStruct.isCreated) 1L else 0L
        val isTaken = if (offerStruct.isTaken) 1L else 0L
        val havePublicKeyLong = if (offer.havePublicKey) 1L else 0L
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
            chainID = offer.chainID.toString(),
            havePublicKey = havePublicKeyLong,
        )
        databaseService.storeOffer(offerForDatabase)
        logger.info("handleOfferOpenedEvent: persistently stored offer ${offer.id}")
        val settlementMethodStrings = offer.onChainSettlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerForDatabase.offerId, offerForDatabase.chainID,
            settlementMethodStrings)
        logger.info("handleOfferOpenedEvent: persistently stored ${settlementMethodStrings.size} settlement " +
                "methods for offer ${offer.id}")
        offerOpenedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.addOffer(offer)
        }
        logger.info("handleOfferOpenedEvent: added offer ${offer.id} to offerTruthSource")
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [OfferEditedEvent]. Once notified,
     * [OfferService] saves [event] in [offerEditedEventRepository], gets updated on-chain offer data by calling
     * [blockchainService]'s [BlockchainService.getOffer] method, verifies that the chain ID of the event and the offer
     * data match, creates an updated [Offer] and with the results, checks if [keyManagerService] has the maker's public
     * key and updates the [Offer.havePublicKey] property accordingly, updates the settlement methods of the
     * corresponding persistently stored offer, removes [event] from [offerEditedEventRepository], and then adds the
     * updated [Offer] to [offerTruthSource] on the main coroutine dispatcher.
     *
     * @param event The [OfferEditedEvent] of which [OfferService] is being notified.
     *
     * @throws [IllegalStateException] if no offer is found with the ID specified in [event], or if the chain ID of [event]
     * doesn't match the chain ID of the offer obtained from [BlockchainService.getOffer] when called with
     * [OfferEditedEvent.offerID].
     */
    override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
        logger.info("handleOfferEditedEvent: handling event for offer ${event.offerID}")
        val encoder = Base64.getEncoder()
        offerEditedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (offerStruct == null) {
            logger.info("No on-chain offer was found with ID specified in OfferEditedEvent in handleOfferEditedEvent " +
                    "call. OfferEditedEvent.id: ${event.offerID}")
            return
        }
        logger.info("handleOfferEditedEvent: got offer ${event.offerID}")
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException("Chain ID of OfferEditedEvent did not match chain ID of OfferStruct in " +
                    "handleOfferEditedEvent call. OfferEditedEvent.chainID: ${event.chainID}, " +
                    "OfferStruct.chainID: ${offerStruct.chainID} OfferEditedEvent.offerID: ${event.offerID}")
        }
        val havePublicKey = (keyManagerService.getPublicKey(offerStruct.interfaceID) != null)
        logger.info("handleOfferOpenedEvent: havePublicKey for offer ${event.offerID}: $havePublicKey")
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
            onChainSettlementMethods = offerStruct.settlementMethods,
            protocolVersion = offerStruct.protocolVersion,
            chainID = offerStruct.chainID,
            havePublicKey = havePublicKey
        )
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(offer.id.mostSignificantBits)
        offerIDByteBuffer.putLong(offer.id.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = encoder.encodeToString(offerIDByteArray)
        val chainIDString = offer.chainID.toString()
        val settlementMethodStrings = offer.onChainSettlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerIdString, chainIDString, settlementMethodStrings)
        logger.info("handleOfferEditedEvent: persistently stored ${settlementMethodStrings.size} updated " +
                "settlement methods for offer ${offer.id}")
        databaseService.updateOfferHavePublicKey(offerIdString, chainIDString, havePublicKey)
        logger.info("handleOfferEditedEvent: persistently updated havePublicKey for offer ${offer.id}")
        offerEditedEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.removeOffer(offer.id)
            offerTruthSource.addOffer(offer)
        }
        logger.info("handleOfferEditedEvent: added updated offer ${offer.id} to offerTruthSource")
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
        logger.info("handleOfferCanceledEvent: handling event for offer ${event.offerID}")
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = Base64.getEncoder().encodeToString(offerIDByteArray)
        offerCanceledEventRepository.append(event)
        databaseService.deleteOffers(offerIdString, event.chainID.toString())
        logger.info("handleOfferCanceledEvent: deleted offer ${event.offerID} from persistent storage")
        databaseService.deleteSettlementMethods(offerIdString, event.chainID.toString())
        logger.info("handleOfferCanceledEvent: deleted settlement methods for offer ${event.offerID} from " +
                "persistent storage")
        offerCanceledEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            if (offerTruthSource.offers[event.offerID]?.chainID == event.chainID) {
                offerTruthSource.removeOffer(event.offerID)
            }
        }
        logger.info("handleOfferCanceledEvent: removed offer ${event.offerID} from offerTruthSource if present")
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
        logger.info("handleOfferTakenEvent: handling event for offer ${event.offerID}")
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIdString = Base64.getEncoder().encodeToString(offerIDByteArray)
        offerTakenEventRepository.append(event)
        databaseService.deleteOffers(offerIdString, event.chainID.toString())
        logger.info("handleOfferTakenEvent: deleted offer ${event.offerID} from persistent storage")
        databaseService.deleteSettlementMethods(offerIdString, event.chainID.toString())
        logger.info("handleOfferTakenEvent: deleted settlement methods for offer ${event.offerID} from " +
                "persistent storage")
        offerTakenEventRepository.remove(event)
        withContext(Dispatchers.Main) {
            if (offerTruthSource.offers[event.offerID]?.chainID == event.chainID) {
                offerTruthSource.removeOffer(event.offerID)
            }
        }
        logger.info("handleOfferTakenEvent: removed offer ${event.offerID} from offerTruthSource if present")
    }

    /**
     * The method called by [com.commuto.interfacedesktop.p2p.P2PService] to notify [OfferService] of a [PublicKeyAnnouncement]. Once notified, [OfferService] checks that the public key in [message] is not already
     * saved in persistent storage via [keyManagerService], and does so if it is not. Then this checks
     * [offerTruthSource] for an offer with the ID specified in [message] and an interface ID equal to that of the
     * public key in [message]. If it finds such an offer, it updates the offer's [Offer.havePublicKey] property to
     * true, to indicate that we have the public key necessary to take the offer and communicate with its maker.
     */
    override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
        logger.info("handlePublicKeyAnnouncement: handling announcement for offer ${message.id}")
        val encoder = Base64.getEncoder()
        if (keyManagerService.getPublicKey(message.publicKey.interfaceId) == null) {
            keyManagerService.storePublicKey(message.publicKey)
            logger.info("handlePublicKeyAnnouncement: persistently stored new public key with interface ID " +
                    "${encoder.encodeToString(message.publicKey.interfaceId)} for offer ${message.id}")
        } else {
            logger.info("handlePublicKeyAnnouncement: already had public key in announcement in persistent " +
                    "storage. Interface ID: ${encoder.encodeToString(message.publicKey.interfaceId)}")
        }
        val offer = withContext(Dispatchers.Main) {
            offerTruthSource.offers[message.id]
        }
        if (offer == null) {
            logger.info("handlePublicKeyAnnouncement: got announcement for offer not in offerTruthSource: " +
                    "${message.id}")
            return
        }
        if (offer.interfaceId.contentEquals(message.publicKey.interfaceId)) {
            withContext(Dispatchers.Main) {
                offerTruthSource.offers[message.id]?.havePublicKey = true
            }
            logger.info("handlePublicKeyAnnouncement: set havePublicKey to true for offer ${offer.id}")
            val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
            offerIDByteBuffer.putLong(message.id.mostSignificantBits)
            offerIDByteBuffer.putLong(message.id.leastSignificantBits)
            val offerIDByteArray = offerIDByteBuffer.array()
            val offerIDString = Base64.getEncoder().encodeToString(offerIDByteArray)
            val chainIDString = offer.chainID.toString()
            databaseService.updateOfferHavePublicKey(offerIDString, chainIDString, true)
            logger.info("handlePublicKeyAnnouncement: persistently set havePublicKey to true for offer " +
                    "${offer.id}")
        } else {
            logger.info("handlePublicKeyAnnouncement: interface ID of public key did not match that of offer " +
                    "${offer.id} specified in announcement. Offer interface ID: ${encoder.encodeToString(offer
                        .interfaceId)}, announcement interface id: ${encoder.encodeToString(message.publicKey
                        .interfaceId)}")
            return
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [ServiceFeeRateChangedEvent]. Once
     * notified, [OfferService] updates [offerTruthSource]'s `serviceFeeRate` property with the value specified in
     * [event] on the main coroutine dispatcher.
     */
    override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
        logger.info("handleServiceFeeRateChangedEvent: handling event. New rate: ${event.newServiceFeeRate}")
        serviceFeeRateChangedEventRepository.append(event)
        withContext(Dispatchers.Main) {
            offerTruthSource.serviceFeeRate.value = event.newServiceFeeRate
        }
        serviceFeeRateChangedEventRepository.remove(event)
        logger.info("handleServiceFeeRateChangedEvent: finished handling event. New rate: " +
                "${event.newServiceFeeRate}")
    }

}