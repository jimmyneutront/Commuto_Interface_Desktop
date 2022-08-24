package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.structs.OfferStruct
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.offer.validation.ValidatedNewOfferData
import com.commuto.interfacedesktop.offer.validation.ValidatedNewSwapData
import com.commuto.interfacedesktop.p2p.OfferMessageNotifiable
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapNotifiable
import com.commuto.interfacedesktop.swap.SwapState
import com.commuto.interfacedesktop.swap.SwapTruthSource
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.ui.offer.OffersViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
 * @property swapService An object adopting [SwapNotifiable] that [OfferService] uses to announce taker information
 * (for offers taken by the user of this interface) or to create [Swap] objects and await the announcement of taker
 * information (for offers that have been taken and were made by the user of this interface).
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
 * @property swapTruthSource The [SwapTruthSource] in which this creates new [Swap]s as necessary.
 * @property blockchainService The [BlockchainService] that this uses to interact with the blockchain.
 * @property p2pService The [P2PService] that this uses for interacting with the peer-to-peer network.
 */
@Singleton
class OfferService (
    private val databaseService: DatabaseService,
    private val keyManagerService: KeyManagerService,
    private val swapService: SwapNotifiable,
    private val offerOpenedEventRepository: BlockchainEventRepository<OfferOpenedEvent>,
    private val offerEditedEventRepository: BlockchainEventRepository<OfferEditedEvent>,
    private val offerCanceledEventRepository: BlockchainEventRepository<OfferCanceledEvent>,
    private val offerTakenEventRepository: BlockchainEventRepository<OfferTakenEvent>,
    private val serviceFeeRateChangedEventRepository: BlockchainEventRepository<ServiceFeeRateChangedEvent>
): OfferNotifiable, OfferMessageNotifiable {

    @Inject constructor(
        databaseService: DatabaseService,
        keyManagerService: KeyManagerService,
        swapService: SwapNotifiable
    ): this(
        databaseService,
        keyManagerService,
        swapService,
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository(),
        BlockchainEventRepository()
    )

    private lateinit var offerTruthSource: OfferTruthSource

    private lateinit var swapTruthSource: SwapTruthSource

    private lateinit var blockchainService: BlockchainService

    private lateinit var p2pService: P2PService

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
     * Used to set the [swapTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [swapTruthSource] property, which cannot be null.
     */
    fun setSwapTruthSource(newTruthSource: SwapTruthSource) {
        check(!::swapTruthSource.isInitialized) {
            "swapTruthSource is already initialized"
        }
        swapTruthSource = newTruthSource
    }

    /**
     * Used to set the [blockchainService] property. This can only be called once.
     *
     * @param newBlockchainService The new value of the [blockchainService] property, which cannot be null.
     */
    fun setBlockchainService(newBlockchainService: BlockchainService) {
        check(!::blockchainService.isInitialized) {
            "blockchainService is already initialized"
        }
        blockchainService = newBlockchainService
    }

    /**
     * Used to set the [p2pService] property. This can only be called once.
     *
     * @param newP2PService The new value of the [p2pService] property, which cannot be null.
     */
    fun setP2PService(newP2PService: P2PService) {
        check(!::p2pService.isInitialized) {
            "p2pService is already initialized"
        }
        p2pService = newP2PService
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
     * Attempts to open a new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer), using the
     * process described in the [interface specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
     *
     * On the IO coroutine dispatcher, this creates and persistently stores a new key pair, creates a new offer ID
     * [UUID] and a new [Offer] from the information contained in [offerData], persistently stores the new [Offer] and
     * its settlement methods, approves token transfer to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract, calls the
     * CommutoSwap contract's [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer)
     * function, passing the new offer ID and [Offer], and updates the state of the offer to
     * [OfferState.OPEN_OFFER_TRANSACTION_BROADCAST]. Finally, on the Main coroutine dispatcher, the new [Offer] is
     * added to [offerTruthSource].
     *
     * @param offerData A [ValidatedNewOfferData] containing the data for the new offer to be opened.
     * @param afterObjectCreation A lambda that will be executed after the new key pair, offer ID and [Offer] object are
     * created.
     * @param afterPersistentStorage A lambda that will be executed after the [Offer] is persistently stored.
     * @param afterTransferApproval A lambda that will be run after the token transfer approval to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract is completed.
     * @param afterOpen A lambda that will be run after the offer is opened, via a call to
     * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer).
     */
    suspend fun openOffer(
        offerData: ValidatedNewOfferData,
        afterObjectCreation: (suspend () -> Unit)? = null,
        afterPersistentStorage: (suspend () -> Unit)? = null,
        afterTransferApproval: (suspend () -> Unit)? = null,
        afterOpen: (suspend () -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            logger.info("openOffer: creating new ID, Offer object and creating and persistently storing new key pair " +
                    "for new offer")
            try {
                // Generate a new 2056 bit RSA key pair for the new offer
                val newKeyPairForOffer = keyManagerService.generateKeyPair(true)
                // Generate a new ID for the offer
                val newOfferID = UUID.randomUUID()
                logger.info("openOffer: created ID $newOfferID for new offer")
                // Create a new Offer
                val newOffer = Offer(
                    isCreated = true,
                    isTaken = false,
                    id = newOfferID,
                    /*
                    It is safe to use the zero address here, because the maker address will be automatically set to that
                    of the function caller by CommutoSwap
                     */
                    maker = "0x0000000000000000000000000000000000000000",
                    interfaceId = newKeyPairForOffer.interfaceId,
                    stablecoin = offerData.stablecoin,
                    amountLowerBound = offerData.minimumAmount,
                    amountUpperBound = offerData.maximumAmount,
                    securityDepositAmount = offerData.securityDepositAmount,
                    serviceFeeRate = offerData.serviceFeeRate,
                    direction = offerData.direction,
                    settlementMethods = mutableStateListOf<SettlementMethod>().apply {
                        offerData.settlementMethods.forEach {
                            this.add(it)
                        }
                    },
                    protocolVersion = BigInteger.ZERO,
                    chainID = BigInteger("31337"),
                    havePublicKey = true,
                    isUserMaker = true,
                    state = OfferState.OPENING,
                )
                afterObjectCreation?.invoke()
                logger.info("openOffer: persistently storing ${newOffer.id}")
                // Persistently store the new offer
                val encoder = Base64.getEncoder()
                val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
                offerIDByteBuffer.putLong(newOffer.id.mostSignificantBits)
                offerIDByteBuffer.putLong(newOffer.id.leastSignificantBits)
                val offerIDByteArray = offerIDByteBuffer.array()
                val offerIDString = encoder.encodeToString(offerIDByteArray)
                val offerForDatabase = DatabaseOffer(
                    isCreated = 1L,
                    isTaken = 0L,
                    id = offerIDString,
                    maker = newOffer.maker,
                    interfaceId = encoder.encodeToString(newOffer.interfaceId),
                    stablecoin = newOffer.stablecoin,
                    amountLowerBound = newOffer.amountLowerBound.toString(),
                    amountUpperBound = newOffer.amountUpperBound.toString(),
                    securityDepositAmount = newOffer.securityDepositAmount.toString(),
                    serviceFeeRate = newOffer.serviceFeeRate.toString(),
                    onChainDirection = newOffer.onChainDirection.toString(),
                    protocolVersion = newOffer.protocolVersion.toString(),
                    chainID = newOffer.chainID.toString(),
                    havePublicKey = 1L,
                    isUserMaker = 1L,
                    state = newOffer.state.asString
                )
                databaseService.storeOffer(offerForDatabase)
                val settlementMethodStrings = newOffer.onChainSettlementMethods.map {
                    encoder.encodeToString(it)
                }
                logger.info("openOffer: persistently storing ${settlementMethodStrings.size} settlement " +
                        "methods for offer ${newOffer.id}")
                databaseService.storeSettlementMethods(offerForDatabase.id, offerForDatabase.chainID,
                    settlementMethodStrings)
                afterPersistentStorage?.invoke()
                afterPersistentStorage?.invoke()
                // Authorize token transfer to CommutoSwap contract
                val tokenAmountForOpeningOffer = newOffer.securityDepositAmount + newOffer.serviceFeeAmountUpperBound
                logger.info("openOffer: authorizing token transfer for ${newOffer.id}. Amount: " +
                        "$tokenAmountForOpeningOffer")
                blockchainService.approveTokenTransferAsync(
                    tokenAddress = newOffer.stablecoin,
                    destinationAddress = blockchainService.getCommutoSwapAddress(),
                    amount = tokenAmountForOpeningOffer
                ).await()
                afterTransferApproval?.invoke()
                logger.info("openOffer: opening ${newOffer.id}")
                blockchainService.openOfferAsync(newOffer.id, newOffer.toOfferStruct()).await()
                logger.info("openOffer: opened ${newOffer.id}")
                newOffer.state = OfferState.OPEN_OFFER_TRANSACTION_BROADCAST
                databaseService.updateOfferState(
                    offerID = offerIDString,
                    chainID = newOffer.chainID.toString(),
                    state = newOffer.state.asString,
                )
                logger.info("openOffer: adding ${newOffer.id} to offerTruthSource")
                withContext(Dispatchers.Main) {
                    offerTruthSource.addOffer(newOffer)
                }
                afterOpen?.invoke()
            } catch (exception: Exception) {
                logger.error("openOffer: encountered exception: $exception", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * On the IO coroutine dispatcher, persistently updates the state of the offer being canceled to
     * [OfferState.CANCELING], and then does the same to the corresponding [Offer] in [offerTruthSource] on the main
     * coroutine dispatcher. Then, back on the IO dispatcher, this cancels the offer on chain by calling the CommutoSwap
     * contract's [cancelOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#cancel-offer) function,
     * passing the offer ID, and then persistently updates the state of the offer to
     * [OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST]. Finally, on the main coroutine dispatcher, this sets the state
     * of the [Offer] in [offerTruthSource] to [OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST]`.
     *
     * @param offerID The ID of the Offer to be canceled.
     * @param chainID The ID of the blockchain on which the Offer exists.
     *
     * @throws [OfferServiceException] if there is an offer in [offerTruthSource] with an ID equal to [offerID] and its
     * [Offer.isTaken] value is false.
     */
    suspend fun cancelOffer(
        offerID: UUID,
        chainID: BigInteger
    ) {
        withContext(Dispatchers.IO) {
            logger.info("cancelOffer: canceling $offerID")
            if (offerTruthSource.offers[offerID]?.isTaken?.value == false) {
                throw OfferServiceException(message = "Offer $offerID is taken and cannot be canceled.")
            }
            try {
                logger.info("cancelOffer: persistently updating offer $offerID state to " +
                        OfferState.CANCELING.asString)
                val encoder = Base64.getEncoder()
                val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
                offerIDByteBuffer.putLong(offerID.mostSignificantBits)
                offerIDByteBuffer.putLong(offerID.leastSignificantBits)
                val offerIDByteArray = offerIDByteBuffer.array()
                val offerIDString = encoder.encodeToString(offerIDByteArray)
                databaseService.updateOfferState(
                    offerID = offerIDString,
                    chainID = chainID.toString(),
                    state = OfferState.CANCELING.asString
                )
                logger.info("cancelOffer: updating offer $offerID state to ${OfferState.CANCELING.asString}")
                withContext(Dispatchers.Main) {
                    offerTruthSource.offers[offerID]?.state = OfferState.CANCELING
                }
                logger.info("cancelOffer: canceling offer $offerID on chain")
                blockchainService.cancelOfferAsync(id = offerID).await()
                logger.info("cancelOffer: persistently updating offer $offerID state to " +
                        OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST.asString)
                databaseService.updateOfferState(
                    offerID = offerIDString,
                    chainID = chainID.toString(),
                    state = OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST.asString
                )
                logger.info("cancelOffer: updating offer $offerID state to " +
                        OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST.asString)
                withContext(Dispatchers.Main) {
                    offerTruthSource.offers[offerID]?.state = OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST
                }
            } catch (exception: Exception) {
                logger.error("openOffer: encountered exception", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * On the IO coroutine dispatcher, this serializes the new settlement methods and calls the CommutoSwap contract's
     * [editOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#edit-offer) function, passing the offer
     * ID and an [OfferStruct] containing the new serialized settlement methods.
     *
     * @param offerID The ID of the offer to edit.
     * @param newSettlementMethods The new settlement methods with which the offer will be edited.
     */
    suspend fun editOffer(offerID: UUID, newSettlementMethods: List<SettlementMethod>) {
        withContext(Dispatchers.IO) {
            logger.info("editOffer: editing $offerID")
            val offer = offerTruthSource.offers[offerID]
                ?: throw OfferServiceException(message = "Offer $offerID was not found.")
            if (offer.isTaken.value) {
                throw OfferServiceException(message = "Offer $offerID is already taken.")
            }
            try {
                logger.info("editOffer: serializing settlement methods for $offerID")
                val onChainSettlementMethods: List<ByteArray> = try {
                    newSettlementMethods.map {
                        Json.encodeToString(it).encodeToByteArray()
                    }
                } catch (exception: Exception) {
                    logger.error("editOffer: encountered error serializing settlement methods for $offerID",
                        exception)
                    throw exception
                }
                logger.info("editOffer: editing $offerID on chain")
                /*
                Since editOffer only uses the settlement methods of the passed Offer struct, we put meaningless values in
                all other properties of the passed struct.
                 */
                val offerStruct = OfferStruct(
                    isCreated = false,
                    isTaken = false,
                    maker = "0x0000000000000000000000000000000000000000",
                    interfaceID = ByteArray(0),
                    stablecoin = "0x0000000000000000000000000000000000000000",
                    amountLowerBound = BigInteger.ZERO,
                    amountUpperBound = BigInteger.ZERO,
                    securityDepositAmount = BigInteger.ZERO,
                    serviceFeeRate = BigInteger.ZERO,
                    direction = BigInteger.ZERO,
                    settlementMethods = onChainSettlementMethods,
                    protocolVersion = BigInteger.ZERO,
                    chainID = BigInteger.ZERO
                )
                blockchainService.editOfferAsync(
                    offerID = offerID,
                    offerStruct = offerStruct
                ).await()
                logger.info("editOffer: successfully edited $offerID")
            } catch (exception: Exception) {
                logger.error("editOffer: encountered error while editing $offerID", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to take an [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer), using the
     * process described in the
     * [interface specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
     *
     * On the IO coroutine dispatcher, this ensures that an offer with an ID equal to that of [offerToTake] exists on
     * chain and is not taken, creates and persistently stores a new key pair and a new [Swap] with the information
     * contained in [offerToTake] and [swapData]. Then, still on the IO coroutine dispatcher, this approves token
     * transfer for the proper amount to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract, calls the
     * CommutoSwap contract's [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer)
     * function (via [BlockchainService]), passing the offer ID and new [Swap], and then updates the state of
     * [offerToTake] to [OfferState.TAKEN] and the state of the swap to [SwapState.TAKE_OFFER_TRANSACTION_BROADCAST].
     * Then, on the main coroutine dispatcher, the new [Swap] is added to [swapTruthSource], the value of
     * [offerToTake]'s [Offer.isTaken] property is set to true and [offerToTake] is removed from [offerTruthSource].
     *
     * @param offerToTake The [Offer] that this function will take.
     * @param swapData A [ValidatedNewSwapData] containing data necessary for taking [offerToTake].
     * @param afterAvailabilityCheck A lambda that will be executed after this has ensured that the offer exists and is
     * not taken.
     * @param afterObjectCreation A lambda that will be executed after the new key pair and [Swap] objects are created.
     * @param afterPersistentStorage A lambda that will be executed after the [Swap] is persistently stored.
     * @param afterTransferApproval A lambda that will be executed after the token transfer approval to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract is completed.
     *
     * @throws OfferServiceException if [offerToTake] does not exist on chain or is already taken, or if the settlement
     * method specified in [swapData] is not accepted by the maker of [offerToTake].
     */
    suspend fun takeOffer(
        offerToTake: Offer,
        swapData: ValidatedNewSwapData,
        afterAvailabilityCheck: (suspend () -> Unit)? = null,
        afterObjectCreation: (suspend () -> Unit)? = null,
        afterPersistentStorage: (suspend () -> Unit)? = null,
        afterTransferApproval: (suspend () -> Unit)? = null,
    ) {
        withContext(Dispatchers.IO) {
            logger.info("takeOffer: checking that ${offerToTake.id} is created and not taken")
            try {
                val encoder = Base64.getEncoder()
                // Try to get the on-chain offer corresponding to offerToTake
                val offerOnChain = blockchainService.getOffer(id = offerToTake.id)
                    ?: throw OfferServiceException("Unable to find on-chain offer with id ${offerToTake.id}")
                if (!offerOnChain.isCreated) {
                    throw OfferServiceException("Offer ${offerToTake.id} does not exist")
                }
                if (offerOnChain.isTaken) {
                    throw OfferServiceException("Offer ${offerToTake.id} has already been taken")
                }
                afterAvailabilityCheck?.invoke()
                logger.info("takeOffer: creating Swap object and creating and persistently storing new key " +
                        "pair to take offer ${offerToTake.id}")
                // Generate a new 2056 bit RSA key pair for the new offer
                val newKeyPairForSwap = keyManagerService.generateKeyPair(true)
                val requiresFill = when (offerToTake.direction) {
                    OfferDirection.BUY -> false
                    OfferDirection.SELL -> true
                }
                val serviceFeeAmount = (swapData.takenSwapAmount * offerToTake.serviceFeeRate) /
                        BigInteger.valueOf(10_000L)
                val selectedOnChainSettlementMethod = offerToTake.onChainSettlementMethods.firstOrNull {
                    try {
                        val settlementMethod = Json.decodeFromString<SettlementMethod>(it.decodeToString())
                        settlementMethod == swapData.settlementMethod
                    } catch (exception: Exception) {
                        logger.warn("takeOffer: got exception while deserializing settlement method " +
                                "${encoder.encodeToString(it)} for ${offerToTake.id}")
                        false
                    }
                }
                    ?: throw OfferServiceException("Unable to find specified settlement method in list of settlement " +
                            "methods accepted by offer maker")
                // TODO: Get proper taker address here
                val newSwap = Swap(
                    isCreated = true,
                    requiresFill = requiresFill,
                    id = offerToTake.id,
                    maker = offerToTake.maker,
                    makerInterfaceID = offerToTake.interfaceId,
                    taker = "0x0000000000000000000000000000000000000000",
                    takerInterfaceID = newKeyPairForSwap.interfaceId,
                    stablecoin = offerToTake.stablecoin,
                    amountLowerBound = offerToTake.amountLowerBound,
                    amountUpperBound = offerToTake.amountUpperBound,
                    securityDepositAmount = offerToTake.securityDepositAmount,
                    takenSwapAmount = swapData.takenSwapAmount,
                    serviceFeeAmount = serviceFeeAmount,
                    serviceFeeRate = offerToTake.serviceFeeRate,
                    direction = offerToTake.direction,
                    onChainSettlementMethod = selectedOnChainSettlementMethod,
                    protocolVersion = offerToTake.protocolVersion,
                    isPaymentSent = false,
                    isPaymentReceived = false,
                    hasBuyerClosed = false,
                    hasSellerClosed = false,
                    onChainDisputeRaiser = BigInteger.ZERO,
                    chainID = offerToTake.chainID,
                    state = SwapState.TAKING,
                )
                afterObjectCreation?.invoke()
                logger.info("takeOffer: persistently storing ${offerToTake.id}")
                // Persistently store the new swap
                val offerIDB64String = encoder.encodeToString(offerToTake.id.asByteArray())
                val swapForDatabase = DatabaseSwap(
                    id = offerIDB64String,
                    isCreated = 1L,
                    requiresFill = 0L,
                    maker = newSwap.maker,
                    makerInterfaceID = encoder.encodeToString(newSwap.makerInterfaceID),
                    taker = newSwap.taker,
                    takerInterfaceID = encoder.encodeToString(newSwap.takerInterfaceID),
                    stablecoin = newSwap.stablecoin,
                    amountLowerBound = newSwap.amountLowerBound.toString(),
                    amountUpperBound = newSwap.amountUpperBound.toString(),
                    securityDepositAmount = newSwap.securityDepositAmount.toString(),
                    takenSwapAmount = newSwap.takenSwapAmount.toString(),
                    serviceFeeAmount = newSwap.serviceFeeAmount.toString(),
                    serviceFeeRate = newSwap.serviceFeeRate.toString(),
                    onChainDirection = newSwap.onChainDirection.toString(),
                    settlementMethod = encoder.encodeToString(newSwap.onChainSettlementMethod),
                    protocolVersion = newSwap.protocolVersion.toString(),
                    isPaymentSent = 0L,
                    isPaymentReceived = 0L,
                    hasBuyerClosed = 0L,
                    hasSellerClosed = 0L,
                    disputeRaiser = newSwap.onChainDisputeRaiser.toString(),
                    chainID = newSwap.chainID.toString(),
                    state = newSwap.state.asString,
                )
                databaseService.storeSwap(swapForDatabase)
                afterPersistentStorage?.invoke()
                val tokenAmountForTakingOffer = when(offerToTake.direction) {
                    /*
                    We are taking a BUY offer, so we are SELLING stablecoin. Therefore we must authorize a transfer
                    equal to the taken swap amount, the security deposit amount, and the service fee amount to the
                    CommutoSwap contract.
                     */
                    OfferDirection.BUY -> newSwap.takenSwapAmount + newSwap.securityDepositAmount +
                            newSwap.serviceFeeAmount
                    OfferDirection.SELL -> newSwap.securityDepositAmount + newSwap.serviceFeeAmount
                }
                logger.info("takeOffer: authorizing transfer for ${offerToTake.id}. Amount: " +
                        "$tokenAmountForTakingOffer")
                blockchainService.approveTokenTransferAsync(
                    tokenAddress = newSwap.stablecoin,
                    destinationAddress = blockchainService.getCommutoSwapAddress(),
                    amount = tokenAmountForTakingOffer,
                ).await()
                afterTransferApproval?.invoke()
                logger.info("takeOffer: taking ${offerToTake.id}")
                blockchainService.takeOfferAsync(
                    id = offerToTake.id,
                    swapStruct = newSwap.toSwapStruct()
                ).await()
                logger.info("takeOffer: took ${offerToTake.id}")
                offerToTake.state = OfferState.TAKEN
                databaseService.updateOfferState(
                    offerID = offerIDB64String,
                    chainID = offerToTake.chainID.toString(),
                    state = OfferState.TAKEN.asString
                )
                newSwap.state = SwapState.TAKE_OFFER_TRANSACTION_BROADCAST
                databaseService.updateSwapState(
                    swapID = offerIDB64String,
                    chainID = newSwap.chainID.toString(),
                    state = SwapState.TAKE_OFFER_TRANSACTION_BROADCAST.asString
                )
                logger.info("takeOffer: adding ${newSwap.id} to swapTruthSource and removing " +
                        "${offerToTake.id} from offerTruthSource")
                withContext(Dispatchers.Main) {
                    swapTruthSource.addSwap(swap = newSwap)
                    offerToTake.isTaken.value = true
                    offerTruthSource.removeOffer(id = offerToTake.id)
                }
                logger.info("takeOffer: removing offer ${offerToTake.id} from persistent storage")
                databaseService.deleteOffers(
                    offerID = offerIDB64String,
                    chainID = offerToTake.chainID.toString()
                )
            } catch (exception: Exception) {
                logger.error("takeOffer: encountered exception during call for ${offerToTake.id}", exception)
                throw exception
            }
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferOpenedEvent].
     *
     * Once notified, [OfferService] saves [event] in offerOpenedEventRepository], gets all on-chain offer data by
     * calling [BlockchainService.getOffer], verifies that the chain ID of the event and the offer data match, and then
     * checks if the offer has been persistently stored in [databaseService]. If it has been persistently stored and if
     * its [DatabaseOffer.isUserMaker] field is true, then the user of this interface has created the offer, and so
     * [OfferService] updates the offer's state to [OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT], announces the
     * corresponding public key by getting its key pair from [keyManagerService] and passing the key pair and the offer
     * ID specified in [event] to [P2PService.announcePublicKey], updates the offer state to
     * [OfferState.OFFER_OPENED], and removes [event]  from [offerOpenedEventRepository]. If the offer has not been
     * persistently stored or if its `isUserMaker` field is false, then [OfferService] creates a new [Offer] and list of
     * settlement methods with the results, checks if [keyManagerService] has the maker's public key and updates the
     * [Offer.havePublicKey] and [Offer.state] properties accordingly, persistently stores the new offer and its
     * settlement methods, removes [event] from [offerOpenedEventRepository], and then adds the new [Offer] to
     * [offerTruthSource] on the main coroutine dispatcher.
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
            logger.info("handleOfferOpenedEvent: No on-chain offer was found with ID specified in OfferOpenedEvent " +
                    "in handleOfferOpenedEvent call. OfferOpenedEvent.id: ${event.offerID}")
            return
        }
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException("Chain ID of OfferOpenedEvent did not match chain ID of OfferStruct in " +
                    "handleOfferOpenedEvent call. OfferOpenedEvent.chainID: ${event.chainID}, " +
                    "OfferStruct.chainID: ${offerStruct.chainID}, OfferOpenedEvent.offerID: ${event.offerID}")
        }
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIDString = encoder.encodeToString(offerIDByteArray)
        val offerInDatabase = databaseService.getOffer(offerIDString)
        /*
        If offerInDatabase is null or isUserMaker is false (0L), this will be false. It will be true if and only if
        offerInDatabase is not null and isUserMaker is true (1L)
         */
        val isUserMaker = offerInDatabase?.isUserMaker == 1L
        if (isUserMaker) {
            logger.info("handleOfferOpenedEvent: offer ${event.offerID} made by the user")
            // The user of this interface is the maker of this offer, so we must announce the public key.
            val keyPair = keyManagerService.getKeyPair(offerStruct.interfaceID)
                ?: throw IllegalStateException("handleOfferOpenedEvent: got null while getting key pair with " +
                        "interface ID ${encoder.encodeToString(offerStruct.interfaceID)} for offer ${event.offerID}, " +
                        "which was made by the user")
            /*
            We do update the state of the persistently stored offer here but not the state of the corresponding Offer in
            offerTruthSource, since the offer should only remain in the awaitingPublicKeyAnnouncement state for a few
            moments.
             */
            databaseService.updateOfferState(
                offerID = offerIDString,
                chainID = event.chainID.toString(),
                state = OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT.asString
            )
            logger.info("handleOfferOpenedEvent: announcing public key for ${event.offerID}")
            p2pService.announcePublicKey(offerID = event.offerID, keyPair = keyPair)
            logger.info("handleOfferOpenedEvent: announced public key for ${event.offerID}")
            databaseService.updateOfferState(
                offerID = offerIDString,
                chainID = event.chainID.toString(),
                state = OfferState.OFFER_OPENED.asString,
            )
            val offerInTruthSource = withContext(Dispatchers.Main) {
                offerTruthSource.offers[event.offerID]
            }
            if (offerInTruthSource != null) {
                withContext(Dispatchers.Main) {
                    offerInTruthSource.state = OfferState.OFFER_OPENED
                }
            } else {
                logger.warn("handleOfferOpenedEvent: offer ${event.offerID} (made by interface user) not " +
                        "found in offerTruthSource during handleOfferOpenedEvent call")
            }
            offerOpenedEventRepository.remove(event)
        } else {
            val havePublicKey = (keyManagerService.getPublicKey(offerStruct.interfaceID) != null)
            logger.info("handleOfferOpenedEvent: havePublicKey for offer ${event.offerID}: $havePublicKey")
            val offerState: OfferState = if (havePublicKey) {
                OfferState.OFFER_OPENED
            } else {
                OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT
            }
            val offer = Offer.fromOnChainData(
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
                isUserMaker = false,
                state = offerState,
            )
            val isCreated = if (offerStruct.isCreated) 1L else 0L
            val isTaken = if (offerStruct.isTaken) 1L else 0L
            val havePublicKeyLong = if (offer.havePublicKey) 1L else 0L
            val isUserMakerLong = if (offer.isUserMaker) 1L else 0L
            val offerForDatabase = DatabaseOffer(
                isCreated = isCreated,
                isTaken = isTaken,
                id = encoder.encodeToString(offerIDByteArray),
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
                isUserMaker = isUserMakerLong,
                state = offer.state.asString
            )
            databaseService.storeOffer(offerForDatabase)
            logger.info("handleOfferOpenedEvent: persistently stored offer ${offer.id}")
            val settlementMethodStrings = offer.onChainSettlementMethods.map {
                encoder.encodeToString(it)
            }
            databaseService.storeSettlementMethods(offerForDatabase.id, offerForDatabase.chainID,
                settlementMethodStrings)
            logger.info("handleOfferOpenedEvent: persistently stored ${settlementMethodStrings.size} settlement " +
                    "methods for offer ${offer.id}")
            offerOpenedEventRepository.remove(event)
            withContext(Dispatchers.Main) {
                offerTruthSource.addOffer(offer)
            }
            logger.info("handleOfferOpenedEvent: added offer ${offer.id} to offerTruthSource")
        }
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of a [OfferEditedEvent].
     *
     * Once notified, [OfferService] saves [event] in [offerEditedEventRepository], gets updated on-chain offer data by
     * calling [BlockchainService.getOffer], verifies that the chain ID of the event and the offer data match, checks
     * for the existence of an offer corresponding to the ID specified in [event] in persistent storage, updates the
     * settlement methods of the corresponding persistently stored offer, updates the settlement methods of the [Offer]
     * with the ID specified in [event] (if present) in [OfferTruthSource.offers] on the main coroutine dispatcher.
     *
     * @param event The [OfferEditedEvent] of which [OfferService] is being notified.
     *
     * @throws [IllegalStateException] if no offer is found with the ID specified in [event], or if the chain ID of
     * [event] doesn't match the chain ID of the offer obtained from [BlockchainService.getOffer] when called with
     * [OfferEditedEvent.offerID].
     */
    override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
        logger.info("handleOfferEditedEvent: handling event for offer ${event.offerID}")
        val encoder = Base64.getEncoder()
        offerEditedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (offerStruct == null) {
            logger.info(
                "No on-chain offer was found with ID specified in OfferEditedEvent in handleOfferEditedEvent call. " +
                        "OfferEditedEvent.id: ${event.offerID}"
            )
            return
        }
        logger.info("handleOfferEditedEvent: got offer ${event.offerID}")
        if (event.chainID != offerStruct.chainID) {
            throw IllegalStateException(
                "Chain ID of OfferEditedEvent did not match chain ID of OfferStruct in " +
                        "handleOfferEditedEvent call. OfferEditedEvent.chainID: ${event.chainID}, " +
                        "OfferStruct.chainID: ${offerStruct.chainID} OfferEditedEvent.offerID: ${event.offerID}"
            )
        }
        logger.info("handleOfferEditedEvent: checking for offer ${event.offerID} in databaseService")
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(event.offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(event.offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIDString = encoder.encodeToString(offerIDByteArray)
        val offerInDatabase = databaseService.getOffer(id = offerIDString)
        if (offerInDatabase == null) {
            logger.warn("handleOfferEditedEvent: could not find persistently stored offer ${event.offerID} " +
                    "during handleOfferEditedEvent call")
        }
        val chainIDString = event.chainID.toString()
        val settlementMethodStrings = offerStruct.settlementMethods.map {
            encoder.encodeToString(it)
        }
        databaseService.storeSettlementMethods(offerIDString, chainIDString, settlementMethodStrings)
        logger.info(
            "handleOfferEditedEvent: persistently stored ${settlementMethodStrings.size} updated settlement methods " +
                    "for offer ${event.offerID}"
        )
        val offer = offerTruthSource.offers[event.offerID]
        if (offer != null) {
            withContext(Dispatchers.Main) {
                offer.updateSettlementMethodsFromChain(
                    onChainSettlementMethods = offerStruct.settlementMethods
                )
            }
            logger.info("handleOfferEditedEvent: updated offer ${event.offerID} in offerTruthSource")
        } else {
            logger.warn("handleOfferEditedEvent: could not find offer ${event.offerID} in offerTruthSource")
        }
        offerEditedEventRepository.remove(event)
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferCanceledEvent].
     *
     * Once notified, [OfferService] saves [event] in [offerCanceledEventRepository], removes the corresponding [Offer]
     * and its settlement methods from persistent storage, removes [event] from [offerCanceledEventRepository], and then
     * checks that the chain ID of the event matches the chain ID of the [Offer] mapped to the offer ID specified in
     * [event] in the [OfferTruthSource.offers] list on the main coroutine dispatcher. If they do not match, this
     * returns. If they do match, then this synchronously removes the [Offer] from said list on the main thread.
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
                offerTruthSource.offers[event.offerID]?.isCreated?.value = false
                offerTruthSource.offers[event.offerID]?.state = OfferState.CANCELED
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
        /*
         If we have in persistent storage a swap with the ID specified in the event, then we are the taker of the offer,
         and so we must announce taker information.
          */
        if (databaseService.getSwap(id = offerIdString) != null) {
            logger.info("handleOfferTakenEvent: ${event.offerID} was taken by the user of this interface, " +
                    "announcing taker info")
            swapService.sendTakerInformationMessage(swapID = event.offerID, chainID = event.chainID)
        } else {
            databaseService.deleteOffers(offerIdString, event.chainID.toString())
            logger.info("handleOfferTakenEvent: deleted offer ${event.offerID} from persistent storage")
            databaseService.deleteSettlementMethods(offerIdString, event.chainID.toString())
            logger.info("handleOfferTakenEvent: deleted settlement methods for offer ${event.offerID} from " +
                    "persistent storage")
            offerTakenEventRepository.remove(event)
            withContext(Dispatchers.Main) {
                if (offerTruthSource.offers[event.offerID]?.chainID == event.chainID) {
                    offerTruthSource.offers[event.offerID]?.isTaken?.value = true
                    offerTruthSource.removeOffer(event.offerID)
                }
            }
            logger.info("handleOfferTakenEvent: removed offer ${event.offerID} from offerTruthSource if present")
        }
    }

    /**
     * The method called by [com.commuto.interfacedesktop.p2p.P2PService] to notify [OfferService] of a
     * [PublicKeyAnnouncement].
     *
     * Once notified, [OfferService] checks that the public key in [message] is not already saved in persistent storage
     * via [keyManagerService], and does so if it is not. Then this checks [offerTruthSource] for an offer with the ID
     * specified in [message] and an interface ID equal to that of the public key in [message]. If it finds such an
     * offer, it checks the offer's [Offer.havePublicKey] and [Offer.state] properties. If [Offer.havePublicKey] is true
     * or [Offer.state] is at or beyond [OfferState.OFFER_OPENED], then it returns because this interface already has
     * the public key for this offer. Otherwise, it updates the offer's [Offer.havePublicKey] property to
     * true, to indicate that we have the public key necessary to take the offer and communicate with its maker, and if
     * the offer has not already passed through the [OfferState.OFFER_OPENED] state, updates its [Offer.state] property
     * to [OfferState.OFFER_OPENED]. It updates these properties in persistent storage as well.
     *
     * @param message The [PublicKeyAnnouncement] of which [OfferService] is being notified.
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
        } else if (offer.havePublicKey || offer.state.indexNumber >= OfferState.OFFER_OPENED.indexNumber) {
            /*
            If we already have the public key for an offer and/or that offer is at or beyond the offerOpened state (such
            as an offer made by this interface's user), then we do nothing.
             */
            logger.info("handlePublicKeyAnnouncement: got announcement for offer for which public key was " +
                    "already obtained: ${offer.id}")
            return
        } else if (offer.interfaceId.contentEquals(message.publicKey.interfaceId)) {
            withContext(Dispatchers.Main) {
                offerTruthSource.offers[message.id]?.havePublicKey = true
                val stateNumberIndex = offerTruthSource.offers[message.id]?.state?.indexNumber
                if (stateNumberIndex != null) {
                    if (stateNumberIndex < OfferState.OFFER_OPENED.indexNumber) {
                        offerTruthSource.offers[message.id]?.state = OfferState.OFFER_OPENED
                    }
                }
            }
            logger.info("handlePublicKeyAnnouncement: set havePublicKey to true for offer ${offer.id}")
            val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
            offerIDByteBuffer.putLong(message.id.mostSignificantBits)
            offerIDByteBuffer.putLong(message.id.leastSignificantBits)
            val offerIDByteArray = offerIDByteBuffer.array()
            val offerIDString = Base64.getEncoder().encodeToString(offerIDByteArray)
            val chainIDString = offer.chainID.toString()
            if (offer.state.indexNumber <= OfferState.OFFER_OPENED.indexNumber) {
                databaseService.updateOfferState(offerIDString, chainIDString, OfferState.OFFER_OPENED.asString)
                logger.info("handlePublicKeyAnnouncement: persistently set state as offerOpened for offer " +
                        "${offer.id}")
            }
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