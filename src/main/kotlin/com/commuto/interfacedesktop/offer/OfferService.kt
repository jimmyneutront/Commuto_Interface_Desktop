package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.blockchain.*
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
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.swap.*
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.ui.offer.OffersViewModel
import com.commuto.interfacedesktop.util.DateFormatter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Numeric
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
 * @property swapService An object adopting [SwapNotifiable] that [OfferService] uses to send taker information
 * (for offers taken by the user of this interface) or to create [Swap] objects and await receiving taker information
 * (for offers that have been taken and were made by the user of this interface).
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
 * @property swapTruthSource The [SwapTruthSource] in which this and [SwapService] are responsible for maintaining an
 * accurate list of swaps. If this is not yet initialized, event handling methods will throw the corresponding error.
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
                    interfaceID = newKeyPairForOffer.interfaceId,
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
                    interfaceId = encoder.encodeToString(newOffer.interfaceID),
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
                    state = newOffer.state.asString,
                    cancelingOfferState = newOffer.cancelingOfferState.value.asString,
                    offerCancellationTransactionHash = newOffer.offerCancellationTransaction?.transactionHash,
                    offerCancellationTransactionCreationTime = null,
                    offerCancellationTransactionCreationBlockNumber = null,
                    editingOfferState = newOffer.editingOfferState.value.asString,
                    offerEditingTransactionHash = newOffer.offerEditingTransaction?.transactionHash,
                    offerEditingTransactionCreationTime = null,
                    offerEditingTransactionCreationBlockNumber = null,
                )
                databaseService.storeOffer(offerForDatabase)
                val settlementMethodStrings = mutableListOf<Pair<String, String?>>()
                newOffer.settlementMethods.forEach {
                    settlementMethodStrings.add(
                        Pair(encoder.encodeToString(it.onChainData), it.privateData)
                    )
                }
                logger.info("openOffer: persistently storing ${settlementMethodStrings.size} settlement " +
                        "methods for offer ${newOffer.id}")
                databaseService.storeOfferSettlementMethods(offerForDatabase.id, offerForDatabase.chainID,
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
                /*logger.info("cancelOffer: persistently updating offer $offerID state to " +
                        OfferState.CANCELING.asString)*/
                val encoder = Base64.getEncoder()
                val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
                offerIDByteBuffer.putLong(offerID.mostSignificantBits)
                offerIDByteBuffer.putLong(offerID.leastSignificantBits)
                val offerIDByteArray = offerIDByteBuffer.array()
                val offerIDString = encoder.encodeToString(offerIDByteArray)
                /*databaseService.updateOfferState(
                    offerID = offerIDString,
                    chainID = chainID.toString(),
                    state = OfferState.CANCELING.asString
                )*/
                /*logger.info("cancelOffer: updating offer $offerID state to ${OfferState.CANCELING.asString}")
                withContext(Dispatchers.Main) {
                    offerTruthSource.offers[offerID]?.state = OfferState.CANCELING
                }*/
                logger.info("cancelOffer: canceling offer $offerID on chain")
                blockchainService.cancelOfferAsync(id = offerID).await()
                /*
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
                }*/
            } catch (exception: Exception) {
                logger.error("openOffer: encountered exception", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] that will cancel an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user of this interface,
     * with the ID specified by [offerID] and on the blockchain specified by [chainID].
     *
     * On the IO coroutine dispatcher, this calls [BlockchainService.createCancelOfferTransaction], passing [offerID]
     * and [chainID], and returns the result.
     *
     * @param offerID The ID of the [Offer] to be canceled.
     * @param chainID The ID of the blockchain on which the [Offer] exists.
     *
     * @return A [RawTransaction] capable of cancelling the offer specified by [offerID] on the blockchain specified by
     * [chainID].
     */
    suspend fun createCancelOfferTransaction(offerID: UUID, chainID: BigInteger): RawTransaction {
        return withContext(Dispatchers.IO) {
            logger.info("createCancelOfferTransaction: creating for $offerID")
            blockchainService.createCancelOfferTransaction(offerID = offerID, chainID = chainID)
        }
    }

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * On the IO coroutine dispatcher, this ensures that the offer is not taken or already being canceled, and that
     * [offerCancellationTransaction] is not `null`. Then it signs [offerCancellationTransaction], creates a
     * [BlockchainTransaction] to wrap it, determines the transaction hash, persistently stores the transaction hash and
     * persistently updates the [Offer.cancelingOfferState] of the offer being canceled to
     * [CancelingOfferState.SENDING_TRANSACTION]. Then, on the main coroutine dispatcher, this stores the transaction
     * hash in [offer] and sets [offer]'s [Offer.cancelingOfferState] property to
     * [CancelingOfferState.SENDING_TRANSACTION]. Then, on the IO coroutine dispatcher, it sends the data derived from
     * signing the [BlockchainTransaction]-wrapped [offerCancellationTransaction] to the blockchain. Once a response is
     * received, this persistently updates the [Offer.cancelingOfferState] of the offer being canceled to
     * [CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION]. Then, on the main coroutine dispatcher, this sets the
     * value of [offer]'s [Offer.cancelingOfferState] to [CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION].
     *
     * @param offer The [Offer] being canceled.
     * @param offerCancellationTransaction An optional [RawTransaction] that can cancel [offer].
     *
     * @throws [OfferServiceException] if [offer] is taken, or if it is already being canceled, or if
     * [offerCancellationTransaction] is `null`.
     */
    suspend fun cancelOffer(offer: Offer, offerCancellationTransaction: RawTransaction?) {
        withContext(Dispatchers.IO) {
            logger.info("cancelOffer: canceling ${offer.id}")
            if (offer.isTaken.value) {
                throw OfferServiceException("Offer ${offer.id} is taken and cannot be canceled.")
            }
            if (offer.cancelingOfferState.value != CancelingOfferState.NONE &&
                offer.cancelingOfferState.value != CancelingOfferState.VALIDATING &&
                offer.cancelingOfferState.value != CancelingOfferState.EXCEPTION) {
                throw OfferServiceException("Offer ${offer.id} is already being canceled.")
            }
            try {
                if (offerCancellationTransaction == null) {
                    throw OfferServiceException("Transaction was null during cancelOffer call for ${offer.id}")
                }
                logger.info("cancelOffer: signing transaction for ${offer.id}")
                val signedTransactionData = blockchainService.signTransaction(
                    transaction = offerCancellationTransaction,
                    chainID = offer.chainID
                )
                val signedTransactionHex = Numeric.toHexString(signedTransactionData)
                val transactionHash = Hash.sha3(signedTransactionHex)
                val blockchainTransactionForOfferCancellation = BlockchainTransaction(
                    transaction = offerCancellationTransaction,
                    transactionHash = transactionHash,
                    latestBlockNumberAtCreation = blockchainService.newestBlockNum,
                    type = BlockchainTransactionType.CANCEL_OFFER,
                )
                val dateString = DateFormatter.createDateString(blockchainTransactionForOfferCancellation
                    .timeOfCreation)
                logger.info("cancelOffer: persistently storing offer cancellation data for ${offer.id}, including tx " +
                        "hash ${blockchainTransactionForOfferCancellation.transactionHash} for ${offer.id}")
                val encoder = Base64.getEncoder()
                databaseService.updateOfferCancellationData(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    transactionHash = blockchainTransactionForOfferCancellation.transactionHash,
                    creationTime = dateString,
                    blockNumber = blockchainTransactionForOfferCancellation.latestBlockNumberAtCreation.toLong()
                )
                logger.info("cancelOffer: persistently cancelingOfferState for ${offer.id} state to " +
                        "SENDING_TRANSACTION")
                databaseService.updateCancelingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = CancelingOfferState.SENDING_TRANSACTION.asString
                )
                logger.info("cancelOffer: updating cancelingOfferState for ${offer.id} state to SENDING_TRANSACTION " +
                        "and storing tx hash ${blockchainTransactionForOfferCancellation.transactionHash} in offer")
                withContext(Dispatchers.Main) {
                    offer.cancelingOfferState.value = CancelingOfferState.SENDING_TRANSACTION
                    offer.offerCancellationTransaction = blockchainTransactionForOfferCancellation
                }
                logger.info("cancelOffer: sending $transactionHash for ${offer.id}")
                blockchainService.sendTransaction(
                    transaction = blockchainTransactionForOfferCancellation,
                    signedRawTransactionDataAsHex = signedTransactionHex,
                    chainID = offer.chainID
                )
                logger.info("cancelOffer: persistently updating cancelingOfferState of ${offer.id} to " +
                        "AWAITING_TRANSACTION_CONFIRMATION")
                databaseService.updateCancelingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION.asString
                )
                logger.info("cancelOffer: updating cancelingOfferState for ${offer.id} to " +
                        "AWAITING_TRANSACTION_CONFIRMATION")
                withContext(Dispatchers.Main) {
                    offer.cancelingOfferState.value = CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION
                }
            } catch (exception: Exception) {
                logger.error("cancelOffer: encountered exception while canceling ${offer.id}, setting " +
                        "cancelingOfferState to EXCEPTION", exception)
                val encoder = Base64.getEncoder()
                databaseService.updateCancelingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = CancelingOfferState.EXCEPTION.asString
                )
                offer.cancelingOfferException = exception
                withContext(Dispatchers.Main) {
                    offer.cancelingOfferState.value = CancelingOfferState.EXCEPTION
                }
                throw exception
            }
        }
    }

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * On the IO coroutine dispatcher, this serializes the new settlement methods, saves them and their private data
     * persistently as pending settlement methods, and calls the CommutoSwap contract's
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
                val serializedSettlementMethodsAndPrivateDetails = try {
                    newSettlementMethods.map {
                        Pair(Json.encodeToString(it), it.privateData)
                    }
                } catch (exception: Exception) {
                    logger.error("editOffer: encountered error serializing settlement methods for $offerID", exception)
                    throw exception
                }
                val encoder = Base64.getEncoder()
                logger.info("persistently storing pending settlement methods for $offerID")
                databaseService.storePendingOfferSettlementMethods(
                    offerID = encoder.encodeToString(offerID.asByteArray()),
                    chainID = offer.chainID.toString(),
                    pendingSettlementMethods = serializedSettlementMethodsAndPrivateDetails
                )
                val onChainSettlementMethods = serializedSettlementMethodsAndPrivateDetails.map {
                    it.first.encodeToByteArray()
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
     * Attempts to create an [RawTransaction] that will edit an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user of this interface
     * with the ID specified by [offerID] and on the blockchain specified by [chainID], using the settlement methods
     * contained in [newSettlementMethods].
     *
     * This serializes [newSettlementMethods] and obtains the desired on-chain data from the serialized result, creates
     * an [OfferStruct] with these results and then calls [BlockchainService.createEditOfferTransaction], passing
     * [offerID], [chainID] and the created [OfferStruct].
     *
     * @param offerID The ID of the [Offer] to be edited.
     * @param chainID The ID of the blockchain on which the [Offer] exists.
     * @param newSettlementMethods The new [SettlementMethod]s with which the offer will be updated.
     *
     * @return A [RawTransaction] capable of editing the offer specified by [offerID] on the blockchain specified by
     * [chainID] using the settlement methods contained in [newSettlementMethods].
     */
    suspend fun createEditOfferTransaction(
        offerID: UUID,
        chainID: BigInteger,
        newSettlementMethods: List<SettlementMethod>
    ): RawTransaction {
        return withContext(Dispatchers.IO) {
            logger.info("createEditOfferTransaction: creating for $offerID")
            logger.info("createEditOfferTransaction: serializing settlement methods for $offerID")
            val onChainSettlementMethods = try {
                val serializedSettlementMethodsAndPrivateDetails = newSettlementMethods.map {
                    Pair(Json.encodeToString(it), it.privateData)
                }
                serializedSettlementMethodsAndPrivateDetails.map {
                    it.first.encodeToByteArray()
                }
            } catch (exception: Exception) {
                logger.error("createEditOfferTransaction: encountered exception serializing settlement methods for " +
                        "$offerID", exception)
                throw exception
            }
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
            logger.info("createEditOfferTransaction: creating for $offerID")
            blockchainService.createEditOfferTransaction(
                offerID = offerID,
                chainID = chainID,
                offerStruct = offerStruct
            )
        }
    }

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * On the IO coroutine dispatcher, this ensures that the offer is not taken, canceled, or already being edited, and
     * that [newSettlementMethods] can be serialized. Then, this creates another [RawTransaction] to edit [offer] using
     * the serialized settlement method data, and then ensures that the data of this transaction matches the data of
     * [offerEditingTransaction]. (If they do not match, then [offerEditingTransaction] was not created with the
     * settlement methods contained in [newSettlementMethods].) Then this signs [offerEditingTransaction] and creates a
     * [BlockchainTransaction] to wrap it. Then this persistently stores the offer editing data for said
     * [BlockchainTransaction], persistently stores the pending settlement methods, and persistently updates the editing
     * offer state of [offer] to [EditingOfferState.SENDING_TRANSACTION]`. Then, on the main coroutine dispatcher, this
     * updates the [Offer.editingOfferState] property of [offer] to [EditingOfferState.SENDING_TRANSACTION] and sets the
     * [Offer.offerEditingTransaction] property of [offer] to said [BlockchainTransaction]. Then, on the IO coroutine
     * dispatcher, this calls [BlockchainService.sendTransaction], passing said [BlockchainTransaction]. When this call
     * returns, this persistently updates the editing offer state of [offer] to
     * [EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION], then on the main coroutine dispatcher updates the
     * [Offer.editingOfferState] property of [offer] to [EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION].
     *
     * @param offer The [Offer] being edited.
     * @param newSettlementMethods The [SettlementMethod]s with which [offerEditingTransaction] should have been made,
     * and with which [offer] will be edited.
     * @param offerEditingTransaction An optional [RawTransaction] that can edit [offer] using the [SettlementMethod]s
     * contained in [newSettlementMethods].
     *
     * @throws [OfferServiceException] if [offer] is already taken or already canceled, or already being edited, if
     * [offerEditingTransaction] is `null` or if the data of [offerEditingTransaction] does not match that of the
     * transaction this function creates using [newSettlementMethods].
     */
    suspend fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>,
        offerEditingTransaction: RawTransaction?
    ) {
        withContext(Dispatchers.IO) {
            logger.info("editOffer: editing ${offer.id}")
            if (offer.isTaken.value) {
                throw OfferServiceException(message = "Offer ${offer.id} is already taken.")
            }
            if (!offer.isCreated.value) {
                throw OfferServiceException(message = "Offer ${offer.id} is already canceled.")
            }
            if (offer.editingOfferState.value != EditingOfferState.VALIDATING) {
                throw OfferServiceException(message = "Offer ${offer.id} is already being edited.")
            }
            try {
                val serializedSettlementMethodsAndPrivateDetails = try {
                    newSettlementMethods.map {
                        Pair(Json.encodeToString(it), it.privateData)
                    }
                } catch (exception: Exception) {
                    logger.error("editOffer: encountered exception serializing settlement methods for ${offer.id}",
                        exception)
                    throw exception
                }
                logger.info("editOffer: recreating EthereumTransaction to edit ${offer.id} to ensure " +
                        "offerEditingTransaction was created with the contents of newSettlementMethods")
                val onChainSettlementMethods = serializedSettlementMethodsAndPrivateDetails.map {
                    it.first.encodeToByteArray()
                }
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
                val recreatedOfferEditingTransaction = blockchainService.createEditOfferTransaction(
                    offerID = offer.id,
                    chainID = offer.chainID,
                    offerStruct = offerStruct
                )
                if (offerEditingTransaction == null) {
                    throw OfferServiceException(message = "Transaction was null during editOffer call for ${offer.id}")
                }
                if (recreatedOfferEditingTransaction.data != offerEditingTransaction.data) {
                    throw OfferServiceException(message = "Data of offerEditingTransaction did not match that of " +
                            "transaction created with newSettlementMethods")
                }
                logger.info("editOffer: signing transaction for ${offer.id}")
                val signedTransactionData = blockchainService.signTransaction(
                    transaction = offerEditingTransaction,
                    chainID = offer.chainID
                )
                val signedTransactionHex = Numeric.toHexString(signedTransactionData)
                val transactionHash = Hash.sha3(signedTransactionHex)
                val blockchainTransactionForOfferEditing = BlockchainTransaction(
                    transaction = offerEditingTransaction,
                    transactionHash = transactionHash,
                    latestBlockNumberAtCreation = blockchainService.newestBlockNum,
                    type = BlockchainTransactionType.EDIT_OFFER,
                )
                val dateString = DateFormatter.createDateString(blockchainTransactionForOfferEditing.timeOfCreation)
                logger.info("editOffer: persistently storing offer editing data for ${offer.id}, including tx " +
                        "hash ${blockchainTransactionForOfferEditing.transactionHash} for ${offer.id}")
                val encoder = Base64.getEncoder()
                databaseService.updateOfferEditingData(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    transactionHash = blockchainTransactionForOfferEditing.transactionHash,
                    creationTime = dateString,
                    blockNumber = blockchainTransactionForOfferEditing.latestBlockNumberAtCreation.toLong()
                )
                // TODO: persistently store new settlement methods along with hash of transaction for offer editing here,
                //  rather than offer ID and chain ID
                databaseService.storePendingOfferSettlementMethods(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    pendingSettlementMethods = serializedSettlementMethodsAndPrivateDetails
                )
                logger.info("editOffer: persistently updating editingOfferState for ${offer.id} to sendingTransaction")
                databaseService.updateEditingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = EditingOfferState.SENDING_TRANSACTION.asString,
                )
                logger.info("editOffer: updating editingOfferState for ${offer.id} to sendingTransaction and storing tx " +
                        "hash ${blockchainTransactionForOfferEditing.transactionHash} in offer")
                withContext(Dispatchers.Main) {
                    offer.editingOfferState.value = EditingOfferState.SENDING_TRANSACTION
                    offer.offerEditingTransaction = blockchainTransactionForOfferEditing
                }
                logger.info("editOffer: sending $transactionHash for ${offer.id}")
                blockchainService.sendTransaction(
                    transaction = blockchainTransactionForOfferEditing,
                    signedRawTransactionDataAsHex = signedTransactionHex,
                    chainID = offer.chainID
                )
                logger.info("persistently updating editingOfferState of ${offer.id} to AWAITING_TRANSACTION_CONFIRMATION")
                databaseService.updateEditingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION.asString
                )
                withContext(Dispatchers.Main) {
                    offer.editingOfferState.value = EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION
                }
            } catch (exception: Exception) {
                logger.error("editOffer: encountered exception while editing ${offer.id}, setting editingOfferState " +
                        "to EXCEPTION", exception)
                val encoder = Base64.getEncoder()
                databaseService.updateEditingOfferState(
                    offerID = encoder.encodeToString(offer.id.asByteArray()),
                    chainID = offer.chainID.toString(),
                    state = EditingOfferState.EXCEPTION.asString
                )
                offer.editingOfferException = exception
                withContext(Dispatchers.Main) {
                    offer.editingOfferState.value = EditingOfferState.EXCEPTION
                }
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
                        settlementMethod.currency == swapData.makerSettlementMethod.currency &&
                                settlementMethod.method ==  swapData.makerSettlementMethod.method &&
                                settlementMethod.price ==  swapData.makerSettlementMethod.price
                    } catch (exception: Exception) {
                        logger.warn("takeOffer: got exception while deserializing settlement method " +
                                "${encoder.encodeToString(it)} for ${offerToTake.id}")
                        false
                    }
                }
                    ?: throw OfferServiceException("Unable to find specified settlement method in list of settlement " +
                            "methods accepted by offer maker")
                // TODO: Get proper taker address here
                val swapRole = when (offerToTake.direction) {
                    // The maker is offering to buy, so we are selling
                    OfferDirection.BUY -> SwapRole.TAKER_AND_SELLER
                    // The maker is offering to sell, so we are buying
                    OfferDirection.SELL -> SwapRole.TAKER_AND_BUYER
                }
                val newSwap = Swap(
                    isCreated = true,
                    requiresFill = requiresFill,
                    id = offerToTake.id,
                    maker = offerToTake.maker,
                    makerInterfaceID = offerToTake.interfaceID,
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
                    role = swapRole,
                )
                newSwap.takerPrivateSettlementMethodData = swapData.takerSettlementMethod.privateData
                afterObjectCreation?.invoke()
                logger.info("takeOffer: persistently storing ${offerToTake.id}")
                // Persistently store the new swap
                val offerIDB64String = encoder.encodeToString(offerToTake.id.asByteArray())
                // TODO: get actual taker private data here
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
                    makerPrivateData = null,
                    makerPrivateDataInitializationVector = null,
                    takerPrivateData = newSwap.takerPrivateSettlementMethodData,
                    takerPrivateDataInitializationVector = null,
                    protocolVersion = newSwap.protocolVersion.toString(),
                    isPaymentSent = 0L,
                    isPaymentReceived = 0L,
                    hasBuyerClosed = 0L,
                    hasSellerClosed = 0L,
                    disputeRaiser = newSwap.onChainDisputeRaiser.toString(),
                    chainID = newSwap.chainID.toString(),
                    state = newSwap.state.value.asString,
                    role = newSwap.role.asString,
                    reportPaymentSentState = newSwap.reportingPaymentSentState.value.toString(),
                    reportPaymentSentTransactionHash = null,
                    reportPaymentSentTransactionCreationTime = null,
                    reportPaymentSentTransactionCreationBlockNumber = null
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
                /*
                The swap object isn't being used in any Composable here, so we don't need to update the state value on
                the main coroutine dispatcher
                 */
                newSwap.state.value = SwapState.TAKE_OFFER_TRANSACTION_BROADCAST
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
                logger.info("takeOffer: removing offer ${offerToTake.id} and its settlement methods from persistent " +
                        "storage")
                databaseService.deleteOffers(
                    offerID = offerIDB64String,
                    chainID = offerToTake.chainID.toString()
                )
                databaseService.deleteOfferSettlementMethods(
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
     * The function called by [BlockchainService] to notify [OfferService] that a monitored offer-related
     * [BlockchainTransaction] has failed (either has been confirmed and failed, or has been dropped.)
     *
     * If [transaction] is of type [BlockchainTransactionType.CANCEL_OFFER], then this finds the offer with the
     * corresponding cancellation transaction hash, persistently updates its canceling offer state to
     * [CancelingOfferState.EXCEPTION] and on the main coroutine dispatcher sets its [Offer.cancelingOfferException]
     * property to [exception], and updates its [Offer.cancelingOfferState] property to [CancelingOfferState.EXCEPTION].
     *
     * If [transaction] is of type [BlockchainTransactionType.EDIT_OFFER], then this finds the offer with the
     * corresponding editing transaction hash, persistently updates its editing offer state to
     * [EditingOfferState.EXCEPTION] and on the main coroutine dispatcher clears its list of selected settlement
     * methods, sets its [Offer.editingOfferException] property to [exception], and updates its
     * [Offer.editingOfferState] property to [EditingOfferState.EXCEPTION]. Then this deletes the offer's pending
     * settlement methods from persistent storage.
     *
     * @param transaction The [BlockchainTransaction] wrapping the on-chain transaction that has failed.
     * @param exception A [BlockchainTransactionException] describing why the on-chain transaction has failed.
     */
    override suspend fun handleFailedTransaction(
        transaction: BlockchainTransaction,
        exception: BlockchainTransactionException
    ) {
        logger.warn("handleFailedTransaction: handling ${transaction.transactionHash} of type ${transaction.type
            .asString} with exception ${exception.message}", exception)
        val encoder = Base64.getEncoder()
        when (transaction.type) {
            BlockchainTransactionType.CANCEL_OFFER -> {
                val offer = offerTruthSource.offers.firstNotNullOfOrNull { uuidOfferEntry ->
                    if (uuidOfferEntry.value.offerCancellationTransaction?.transactionHash
                        .equals(transaction.transactionHash)) {
                        uuidOfferEntry.value
                    } else {
                        null
                    }
                }
                if (offer != null) {
                    logger.warn("handleFailedTransaction: found offer ${offer.id} on ${offer.chainID} with " +
                            "cancellation transaction ${transaction.transactionHash} updating cancelingOfferState to " +
                            "EXCEPTION in persistent storage")
                    databaseService.updateCancelingOfferState(
                        offerID = encoder.encodeToString(offer.id.asByteArray()),
                        chainID = offer.chainID.toString(),
                        state = CancelingOfferState.EXCEPTION.asString,
                    )
                    logger.warn("handleFailedTransaction: setting cancelingOfferException and updating " +
                            "cancelingOfferState to EXCEPTION for ${transaction.transactionHash}")
                    withContext(Dispatchers.Main) {
                        offer.cancelingOfferException = exception
                        offer.cancelingOfferState.value = CancelingOfferState.EXCEPTION
                    }
                } else {
                    logger.warn("handleFailedTransaction: offer with cancellation transaction " +
                            "${transaction.transactionHash} not found in offerTruthSource")
                }
            }
            BlockchainTransactionType.EDIT_OFFER -> {
                val offer = offerTruthSource.offers.firstNotNullOfOrNull { uuidOfferEntry ->
                    if (uuidOfferEntry.value.offerEditingTransaction?.transactionHash
                        .equals(transaction.transactionHash)) {
                        uuidOfferEntry.value
                    } else {
                        null
                    }
                }
                if (offer != null) {
                    logger.warn("handleFailedTransaction: found offer ${offer.id} on ${offer.chainID} with editing " +
                            "transaction ${transaction.transactionHash}, updating editingOfferState to EXCEPTION in " +
                            "persistent storage")
                    databaseService.updateEditingOfferState(
                        offerID = encoder.encodeToString(offer.id.asByteArray()),
                        chainID = offer.chainID.toString(),
                        state = EditingOfferState.EXCEPTION.asString,
                    )
                    logger.warn("handleFailedTransaction: setting editingOfferException and updating " +
                            "editingOfferState to EXCEPTION for ${offer.id} and clearing selected settlement methods")
                    withContext(Dispatchers.Main) {
                        offer.selectedSettlementMethods.clear()
                        offer.editingOfferException = exception
                        offer.editingOfferState.value = EditingOfferState.EXCEPTION
                    }
                    logger.warn("handleFailedTransaction: deleting pending settlement methods for ${offer.id}")
                    databaseService.deletePendingOfferSettlementMethods(
                        offerID = encoder.encodeToString(offer.id.asByteArray()),
                        chainID = offer.chainID.toString()
                    )
                } else {
                    logger.warn("handleFailedTransaction: offer with editing transaction ${transaction
                        .transactionHash} not found in offerTruthSource")
                }
            }
            BlockchainTransactionType.REPORT_PAYMENT_SENT -> {
                throw OfferServiceException(message = "handleFailedTransaction: received a swap-related transaction " +
                        transaction.transactionHash
                )
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
            val offerCancellationTransactionCreationTime = offer.offerCancellationTransaction?.timeOfCreation
            val offerCancellationTransactionCreationTimeString = if (offerCancellationTransactionCreationTime != null) {
                DateFormatter.createDateString(offerCancellationTransactionCreationTime)
            } else { null }
            val offerEditingTransactionCreationTime = offer.offerEditingTransaction?.timeOfCreation
            val offerEditingTransactionCreationTimeString = if (offerEditingTransactionCreationTime != null) {
                DateFormatter.createDateString(offerEditingTransactionCreationTime)
            } else { null }
            val offerForDatabase = DatabaseOffer(
                isCreated = isCreated,
                isTaken = isTaken,
                id = encoder.encodeToString(offerIDByteArray),
                maker = offer.maker,
                interfaceId = encoder.encodeToString(offer.interfaceID),
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
                state = offer.state.asString,
                cancelingOfferState = offer.cancelingOfferState.value.asString,
                offerCancellationTransactionHash = offer.offerCancellationTransaction?.transactionHash,
                offerCancellationTransactionCreationTime = offerCancellationTransactionCreationTimeString,
                offerCancellationTransactionCreationBlockNumber =
                offer.offerCancellationTransaction?.latestBlockNumberAtCreation?.toLong(),
                editingOfferState = offer.editingOfferState.value.asString,
                offerEditingTransactionHash = offer.offerEditingTransaction?.transactionHash,
                offerEditingTransactionCreationTime = offerEditingTransactionCreationTimeString,
                offerEditingTransactionCreationBlockNumber =
                offer.offerEditingTransaction?.latestBlockNumberAtCreation?.toLong()
            )
            databaseService.storeOffer(offerForDatabase)
            logger.info("handleOfferOpenedEvent: persistently stored offer ${offer.id}")
            val settlementMethodStrings = mutableListOf<Pair<String, String?>>()
            offer.settlementMethods.forEach {
                settlementMethodStrings.add(
                    Pair(encoder.encodeToString(it.onChainData), it.privateData)
                )
            }
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
     * calling [BlockchainService.getOffer], and verifies that the chain ID of the event and the offer data match. Then
     * this checks if the user of the interface is the maker of the offer being edited. If so, then the new settlement
     * methods for the offer and their associated private data should be stored in the pending settlement methods
     * database table. So this gets that data from [databaseService], and then deserializes the data to
     * [SettlementMethod] objects and adds them to a list, logging warnings when such a [SettlementMethod] has no
     * associated private data. Then this creates another list of [SettlementMethod] objects that it creates by
     * deserializing the settlement method data in the new on-chain offer data. Then this iterates through the latter
     * list of [SettlementMethod] objects, searching for matching [SettlementMethod] objects in the former list
     * (matching meaning that price, currency, and fiat currency values are equal; obviously on-chain settlement methods
     * will have no private data). If, for a given [SettlementMethod] in the latter list, a matching [SettlementMethod]
     * (which definitely has associated private data) is found in the former list, the matching element in the former
     * list is added to a third list of [SettlementMethod]s. Then, this persistently stores the third list of new
     * [SettlementMethod]s as the offer's settlement methods. Then this checks whether the corresponding [Offer] has a
     * non-`null` [Offer.offerEditingTransaction] property. If it does, then this compares the transaction hash of the
     * value of that property to that of [event]. If they match, this sets a flag indicating that these transaction
     * hashes match. If this flag is set, this persistently updates [Offer]s [Offer.editingOfferState] property to
     * [EditingOfferState.COMPLETED]. Then, on the main coroutine dispatcher, if the matching transaction hash flag has
     * been set, this updates the corresponding [Offer]'s [Offer.editingOfferState] to [EditingOfferState.COMPLETED] and
     * clears the [Offer]'s [Offer.selectedSettlementMethods] list. Then, still on the main coroutine dispatcher,
     * regardless of the status of the flag, this sets the corresponding [Offer]'s settlement methods equal to the
     * contents of the third list of new [SettlementMethod]s.
     *
     * If the user of this interface is not the maker of the offer being edited, this creates a new list of
     * [SettlementMethod]s by deserializing the settlement method data in the new on-chain offer data, persistently
     * stores the list of new [SettlementMethod]`s as the offer's settlement methods, and sets the corresponding
     * [Offer]'s settlement methods equal to the contents of this list on the main coroutine dispatcher.
     *
     * @param event The [OfferEditedEvent] of which [OfferService] is being notified.
     *
     * @throws [OfferServiceException] if the chain ID of [event] doesn't match the chain ID of the offer obtained from
     * [BlockchainService.getOffer] when called with [OfferEditedEvent.offerID].
     */
    override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
        logger.info("handleOfferEditedEvent: handling event for offer ${event.offerID}")
        offerEditedEventRepository.append(event)
        val offerStruct = blockchainService.getOffer(event.offerID)
        if (offerStruct == null) {
            logger.info("No on-chain offer was found with ID specified in OfferEditedEvent in handleOfferEditedEvent " +
                    "call. OfferEditedEvent.id: ${event.offerID}")
            return
        }
        logger.info("handleOfferEditedEvent: got offer ${event.offerID}")
        if (event.chainID != offerStruct.chainID) {
            throw OfferServiceException(
                "Chain ID of OfferEditedEvent did not match chain ID of OfferStruct in " +
                        "handleOfferEditedEvent call. OfferEditedEvent.chainID: ${event.chainID}, " +
                        "OfferStruct.chainID: ${offerStruct.chainID} OfferEditedEvent.offerID: ${event.offerID}"
            )
        }
        val encoder = Base64.getEncoder()
        val offerIDB64String = encoder.encodeToString(event.offerID.asByteArray())
        val chainIDString = event.chainID.toString()
        val offer = offerTruthSource.offers[event.offerID]
        if (offer != null && offer.isUserMaker) {
            logger.info("handleOfferEditedEvent: ${event.offerID} was made by interface user")
            /*
            The user of this interface is the maker of this offer, and therefore we should have pending settlement
            methods for this offer in persistent storage.
             */
            val newSettlementMethods = mutableListOf<SettlementMethod>()
            val pendingSettlementMethods = databaseService.getPendingOfferSettlementMethods(
                offerID = offerIDB64String,
                chainID = chainIDString,
            )
            val deserializedPendingSettlementMethods = mutableListOf<SettlementMethod>()
            if (pendingSettlementMethods != null) {
                if (pendingSettlementMethods.size != offerStruct.settlementMethods.size) {
                    logger.warn("handleOfferEditedEvent: mismatching pending settlement methods counts for " +
                            "${event.offerID}: ${pendingSettlementMethods.size} pending settlement methods in " +
                            "persistent storage, ${offerStruct.settlementMethods.size} settlement methods on-chain")
                }
                for (pendingSettlementMethod in pendingSettlementMethods) {
                    try {
                        val deserializedPendingSettlementMethod =
                            Json.decodeFromString<SettlementMethod>(pendingSettlementMethod.first)
                        deserializedPendingSettlementMethod.privateData = pendingSettlementMethod.second
                        if (deserializedPendingSettlementMethod.privateData == null) {
                            logger.warn("handleOfferEditedEvent: did not find private data for pending settlement " +
                                    "method ${pendingSettlementMethod.first} for ${event.offerID}")
                        }
                        deserializedPendingSettlementMethods.add(deserializedPendingSettlementMethod)
                    } catch (exception: Exception) {
                        logger.warn("handleOfferEditedEvent: encountered exception while deserializing " +
                                "pending settlement method ${pendingSettlementMethod.first} for ${event.offerID}")
                    }
                }
            } else {
                logger.warn("handleOfferEditedEvent: found no pending settlement methods for ${event.offerID}")
            }

            for (onChainSettlementMethod in offerStruct.settlementMethods) {
                try {
                    val onChainSettlementMethodUTF8String = onChainSettlementMethod.decodeToString()
                    val deserializedOnChainSettlementMethod = Json.decodeFromString<SettlementMethod>(
                        onChainSettlementMethodUTF8String)
                    val correspondingDeserializedPendingSettlementMethod = deserializedPendingSettlementMethods
                        .firstOrNull {
                            deserializedOnChainSettlementMethod.currency == it.currency &&
                                    deserializedOnChainSettlementMethod.price == it.price &&
                                    deserializedOnChainSettlementMethod.method == it.method
                        }
                    if (correspondingDeserializedPendingSettlementMethod != null) {
                        newSettlementMethods.add(correspondingDeserializedPendingSettlementMethod)
                    } else {
                        logger.warn("handleOfferEditedEvent: unable to find pending settlement method for " +
                                "on-chain settlement method $onChainSettlementMethodUTF8String for ${event.offerID}")
                    }
                } catch (exception: Exception) {
                    logger.warn("handleOfferEditedEvent: encountered exception while deserializing on-chain " +
                            "settlement method ${encoder.encodeToString(onChainSettlementMethod)} for ${event.offerID}")
                }
            }

            if (newSettlementMethods.size != offerStruct.settlementMethods.size) {
                logger.warn("handleOfferEditedEvent: mismatching new settlement methods counts for " +
                        "${event.offerID}: ${newSettlementMethods.size} new settlement methods, ${offerStruct
                            .settlementMethods.size} settlement methods on-chain")
            }

            val newSerializedSettlementMethods = newSettlementMethods.map {
                /*
                Since we just deserialized these settlement methods, we should never get an error while re-serializing
                them again
                 */
                Pair(Json.encodeToString(it), it.privateData)
            }
            databaseService.storeOfferSettlementMethods(
                offerID = offerIDB64String,
                chainID = chainIDString,
                settlementMethods = newSerializedSettlementMethods
            )
            logger.info("handleOfferEditedEvent: persistently stored ${newSerializedSettlementMethods.size} " +
                    "settlement methods for ${event.offerID}")
            databaseService.deletePendingOfferSettlementMethods(offerID = offerIDB64String, chainID = chainIDString)
            logger.info("handleOfferEditedEvent: removed pending settlement methods from persistent storage " +
                    "for ${event.offerID}")
            var gotExpectedOfferEditingTransaction = false
            val offerEditingTransaction = offer.offerEditingTransaction
            if (offerEditingTransaction != null) {
                if (offerEditingTransaction.transactionHash == event.transactionHash) {
                    logger.info("handleOfferEditedEvent: tx hash ${event.transactionHash} of event matches that " +
                            "for offer ${event.offerID}: ${offerEditingTransaction.transactionHash}")
                    gotExpectedOfferEditingTransaction = true
                } else {
                    logger.warn("handleOfferEditedEvent: tx hash ${event.transactionHash} does not match that for " +
                            "offer ${event.offerID}: ${offerEditingTransaction.transactionHash}")
                }
            } else {
                logger.warn("handleOfferEditedEvent: offer ${event.offerID} made by the interface user has no offer " +
                        "editing transaction")
            }
            if (gotExpectedOfferEditingTransaction) {
                logger.info("handleOfferEditedEvent: persistently updating editing offer state of " +
                        "${event.offerID} to COMPLETED")
                databaseService.updateEditingOfferState(
                    offerID = encoder.encodeToString(event.offerID.asByteArray()),
                    chainID = event.chainID.toString(),
                    state = EditingOfferState.COMPLETED.asString
                )
            }
            logger.info("handleOfferEditedEvent: updating offer ${event.offerID} to offerTruthSource")
            withContext(Dispatchers.Main) {
                if (gotExpectedOfferEditingTransaction) {
                    offer.editingOfferState.value = EditingOfferState.COMPLETED
                    offer.selectedSettlementMethods.clear()
                }
                offer.updateSettlementMethods(settlementMethods = newSettlementMethods)
            }
            logger.info("handleOfferEditedEvent: updated offer ${event.offerID} in offerTruthSource")
        } else {
            logger.info("handleOfferEditedEvent: ${event.offerID} was not made by interface user")
            val settlementMethodStrings = offerStruct.settlementMethods.map {
                Pair<String, String?>(it.decodeToString(), null)
            }
            databaseService.storeOfferSettlementMethods(
                offerID = offerIDB64String,
                chainID = chainIDString,
                settlementMethods = settlementMethodStrings
            )
            logger.info("handleOfferEditedEvent: persistently stored ${settlementMethodStrings.size} " +
                    "settlement methods for ${event.offerID}")
            if (offer != null) {
                withContext(Dispatchers.Main) {
                    offer.updateSettlementMethodsFromChain(onChainSettlementMethods = offerStruct.settlementMethods)
                }
                logger.info("handleOfferEditedEvent: updated offer ${event.offerID} in offerTruthSource")
            } else {
                logger.warn("handleOfferEditedEvent: could not find offer ${event.offerID} in offerTruthSource")
            }
        }
        offerEditedEventRepository.remove(event)
    }

    /**
     * The method called by [BlockchainService] to notify [OfferService] of an [OfferCanceledEvent]. Once notified,
     * [OfferService] saves [event] in [offerCanceledEventRepository], and checks for the corresponding [Offer] (with an
     * ID and chain ID matching that specified in [event]) in [offerTruthSource]. If it finds such an [Offer], it checks
     * whether the offer was made by the user of this interface. If it was, this checks whether the hash of the offer's
     * [Offer.offerCancellationTransaction] equals that of [event]. If they do not match, then this updates the [Offer],
     * both in [offerTruthSource] and in persistent storage, with a new [BlockchainTransaction] containing the hash
     * specified in [event]. Then, regardless of whether the hashes match, this sets the offer's
     * [Offer.cancelingOfferState] to [CancelingOfferState.COMPLETED] on the main coroutine dispatcher. Then, regardless
     * of whether the [Offer] was or was not made by the user of this interface, this sets the offer's [Offer.isCreated]
     * flag to `false` and its [Offer.state] to [OfferState.CANCELED], and then removes the offer from
     * [offerTruthSource] on the main Dispatch Queue. Finally, regardless of whether an [Offer] was found in
     * [offerTruthSource], this removes the offer with the ID and chain ID specified in [event] (and its settlement
     * methods) from persistent storage. Finally, this removes [event] from [offerCanceledEventRepository].
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
        logger.info("handleOfferCanceledEvent: persistently updating state for ${event.offerID}")
        databaseService.updateOfferState(
            offerID = offerIdString,
            chainID = event.chainID.toString(),
            state = OfferState.CANCELED.asString
        )
        val offer = offerTruthSource.offers[event.offerID]
        if (offer != null && offer.chainID == event.chainID) {
            logger.info("handleOfferCanceledEvent: found offer ${event.offerID} in offerTruthSource")
            if (offer.isUserMaker) {
                var mustUpdateOfferCancellationTransaction = false
                logger.info("handleOfferCanceledEvent: offer ${event.offerID} made by interface user")
                val offerCancellationTransaction = offer.offerCancellationTransaction
                if (offerCancellationTransaction != null) {
                    if (offerCancellationTransaction.transactionHash == event.transactionHash) {
                        logger.info("handleOfferCanceledEvent: tx hash ${event.transactionHash} of event matches " +
                                "that for offer ${event.offerID}: ${offerCancellationTransaction.transactionHash}")
                    } else {
                        logger.warn("handleOfferCanceledEvent: tx hash ${event.transactionHash} of event does not " +
                                "match that for offer ${event.offerID}: ${offerCancellationTransaction
                                    .transactionHash}, updating with new transaction hash")
                        mustUpdateOfferCancellationTransaction = true
                    }
                } else {
                    logger.warn("handleOfferCanceledEvent: offer ${event.offerID} made by interface user has no " +
                            "offer cancellation transaction, updating with transaction hash")
                    mustUpdateOfferCancellationTransaction = true
                }
                if (mustUpdateOfferCancellationTransaction) {
                    val updatedOfferCancellationTransaction = BlockchainTransaction(
                        transactionHash = event.transactionHash,
                        timeOfCreation = Date(),
                        latestBlockNumberAtCreation = BigInteger.ZERO,
                        type = BlockchainTransactionType.CANCEL_OFFER
                    )
                    withContext(Dispatchers.Main) {
                        offer.offerCancellationTransaction = updatedOfferCancellationTransaction
                    }
                    logger.warn("handleOfferCanceledEvent: persistently storing tx hash ${event.transactionHash} " +
                            "for ${event.offerID}")
                    val dateString = DateFormatter.createDateString(updatedOfferCancellationTransaction.timeOfCreation)
                    databaseService.updateOfferCancellationData(
                        offerID = offerIdString,
                        chainID = event.chainID.toString(),
                        transactionHash = updatedOfferCancellationTransaction.transactionHash,
                        creationTime = dateString,
                        blockNumber = updatedOfferCancellationTransaction.latestBlockNumberAtCreation.toLong()
                    )
                }
                logger.info("handleOfferCanceledEvent: updating cancelingOfferState of user-as-maker offer " +
                        "${event.offerID} to ${CancelingOfferState.COMPLETED.asString}")
                withContext(Dispatchers.Main) {
                    offer.cancelingOfferState.value = CancelingOfferState.COMPLETED
                }
            }
            logger.info("handleOfferCanceledEvent: updating state of ${event.offerID} to " +
                    OfferState.CANCELED.asString
            )
            withContext(Dispatchers.Main) {
                offer.isCreated.value = false
                offer.state = OfferState.CANCELED
            }
            logger.info("handleOfferCanceledEvent: removing ${event.offerID} from offerTruthSource")
            withContext(Dispatchers.Main) {
                offerTruthSource.removeOffer(event.offerID)
            }
        }
        logger.info("handleOfferCanceledEvent: removing ${event.offerID} with chain ID ${event.chainID} from " +
                "persistent storage")
        databaseService.deleteOffers(
            offerID = offerIdString,
            chainID = event.chainID.toString()
        )
        logger.info("handleOfferCanceledEvent: removing settlement methods for ${event.offerID} with chain ID " +
                "${event.chainID} from persistent storage")
        databaseService.deleteOfferSettlementMethods(
            offerID = offerIdString,
            chainID = event.chainID.toString()
        )
        offerCanceledEventRepository.remove(event)
    }

    /**
     * The function called by [BlockchainService] to notify [OfferService] of an [OfferTakenEvent]. Once notified,
     * [OfferService] saves [event] in [offerTakenEventRepository] and calls [SwapService.sendTakerInformationMessage],
     * passing the swap ID and chain ID in [event]. If this call returns true, then the user of this interface is the
     * taker of this offer and all necessary action has been taken. Otherwise, the user of this interface is not the
     * taker of this offer, and therefore this searches for an [Offer] in [offerTruthSource] with an ID equal to that
     * specified in [event]. If this finds such an [Offer], this ensures that the chain ID of the offer and the chain ID
     * specified in [event] match, and then checks if the user of this interface is the maker of the offer. If so, this
     * calls [SwapService.handleNewSwap]. Then, regardless of whether the user of this interface is the maker of the
     * offer, this removes the corresponding offer and its settlement methods from persistent storage, and then
     * synchronously removes the [Offer] from `offersTruthSource` on the main coroutine dispatcher. Finally, regardless
     * of whether the user of this interface is the maker or taker of this offer or neither, this removes [event] from
     * [offerTakenEventRepository].
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
        We try to send taker information for the swap with the ID specified in the event. If we cannot (possibly because
        we are not the taker), sendTakerInformationMessage will NOT send a message and will return false, and we handle
        other possible cases
          */
        logger.info("handleOfferTakenEvent: checking role for ${event.offerID}")
        if (swapService.sendTakerInformationMessage(swapID = event.offerID, chainID = event.chainID)) {
            logger.info("handleOfferTakenEvent: sent taker info for ${event.offerID}")
        } else {
            val offer = offerTruthSource.offers[event.offerID]
            if (offer == null) {
                logger.info("handleOfferTakenEvent: got event for offer ${event.offerID} not found in offerTruthSource")
                offerTakenEventRepository.remove(event)
                return
            }
            if (offer.chainID == event.chainID) {
                // If we have the offer and we are the maker, then we handle the new swap
                if (offer.isUserMaker) {
                    logger.info("handleOfferTakenEvent: ${event.offerID} was made by the user of this interface, " +
                            "handling new swap")
                    swapService.handleNewSwap(takenOffer = offer)
                }
                /*
                Regardless of whether we are or are not the maker of this offer, we are not the taker, so we remove the
                offer and its settlement methods.
                 */
                databaseService.deleteOffers(offerID = offerIdString, chainID = event.chainID.toString())
                logger.info("handleOfferTakenEvent: deleted offer ${event.offerID} from persistent storage")
                databaseService.deleteOfferSettlementMethods(
                    offerID = offerIdString,
                    chainID = event.chainID.toString()
                )
                logger.info("handleOfferTakenEvent: deleted settlement methods of offer ${event.offerID} from " +
                        "persistent storage")
                withContext(Dispatchers.Main) {
                    offer.isTaken.value = true
                    offerTruthSource.removeOffer(event.offerID)
                }
                logger.info("handleOfferTakenEvent: removed offer ${event.offerID} from offerTruthSource if " +
                        "present")
            } else {
                logger.info("handleOfferTakenEvent: chain ID ${event.chainID} did not match chain ID of offer " +
                        "${event.offerID}")
            }
        }
        offerTakenEventRepository.remove(event)
    }

    /**
     * The method called by [com.commuto.interfacedesktop.p2p.P2PService] to notify [OfferService] of a
     * [PublicKeyAnnouncement].
     *
     * Once notified, [OfferService] checks for a key pair in [keyManagerService] with an interface ID equal to that of
     * the public key contained in [message]. If such a key pair is present, then this returns, to avoid storing the
     * user's own public key. Otherwise, if no such key pair exists in [keyManagerService], this checks that the public
     * key in [message] is not already saved in persistent storage via [keyManagerService], and does so if it is not.
     * Then this checks [offerTruthSource] for an offer with the ID specified in [message] and an interface ID equal to
     * that of the public key in [message]. If it finds such an offer, it checks the offer's [Offer.havePublicKey] and
     * [Offer.state] properties. If [Offer.havePublicKey] is true or [Offer.state] is at or beyond
     * [OfferState.OFFER_OPENED], then it returns because this interface already has the public key for this offer.
     * Otherwise, it updates the offer's [Offer.havePublicKey] property to true, to indicate that we have the public key
     * necessary to take the offer and communicate with its maker, and if the offer has not already passed through the
     * [OfferState.OFFER_OPENED] state, updates its [Offer.state] property to [OfferState.OFFER_OPENED]. It updates
     * these properties in persistent storage as well.
     *
     * @param message The [PublicKeyAnnouncement] of which [OfferService] is being notified.
     */
    override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {
        logger.info("handlePublicKeyAnnouncement: handling announcement for offer ${message.id}")
        if (keyManagerService.getKeyPair(message.publicKey.interfaceId) != null) {
            logger.info("handlePublicKeyAnnouncement: detected announcement for ${message.id} made by the " +
                    "user of this interface")
            return
        }
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
        } else if (offer.interfaceID.contentEquals(message.publicKey.interfaceId)) {
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
                        .interfaceID)}, announcement interface id: ${encoder.encodeToString(message.publicKey
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