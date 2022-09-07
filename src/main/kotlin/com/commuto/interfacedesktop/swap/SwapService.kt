package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.SwapFilledEvent
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.SwapMessageNotifiable
import com.commuto.interfacedesktop.p2p.messages.MakerInformationMessage
import com.commuto.interfacedesktop.p2p.messages.TakerInformationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Swap Service. It is responsible for processing and organizing swap-related data that it receives from
 * [BlockchainService], [P2PService] and [OfferService] in order to maintain an accurate list of all swaps in a
 * [SwapTruthSource].
 *
 * @property databaseService The [DatabaseService] that this [SwapService] uses for persistent storage.
 * @property keyManagerService The [KeyManagerService] that this [SwapService] will use for managing keys.
 * @property swapTruthSource The [SwapTruthSource] in which this is responsible for maintaining an accurate list of
 * swaps. If this is not yet initialized, event handling methods will throw the corresponding error.
 * @property blockchainService The [BlockchainService] that this uses to interact with the blockchain.
 * @property p2pService The [P2PService] that this uses for interacting with the peer-to-peer network.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 */
@Singleton
class SwapService @Inject constructor(
    private val databaseService: DatabaseService,
    private val keyManagerService: KeyManagerService,
): SwapNotifiable, SwapMessageNotifiable {

    private lateinit var swapTruthSource: SwapTruthSource

    private lateinit var blockchainService: BlockchainService

    private lateinit var p2pService: P2PService

    private val logger = LoggerFactory.getLogger(javaClass)

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
     * Sends a
     * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * for an offer taken by the user of this interface, with an ID equal to [swapID] and a blockchain ID equal to
     * [chainID].
     *
     * This updates the state of the swap with the specified ID (both persistently and in [swapTruthSource]) to
     * [SwapState.AWAITING_TAKER_INFORMATION], creates and sends a Taker Information Message, and then updates the state
     * of the swap with the specified ID (both persistently and in [swapTruthSource]) to
     * [SwapState.AWAITING_MAKER_INFORMATION].
     *
     * @param swapID The ID of the swap for which taker information will be sent.
     * @param chainID The ID of the blockchain on which the swap exists.
     */
    override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {
        logger.info("sendTakerInformationMessage: preparing to send for $swapID")
        val encoder = Base64.getEncoder()
        val swapIDString = encoder.encodeToString(swapID.asByteArray())
        databaseService.updateSwapState(
            swapID = swapIDString,
            chainID = chainID.toString(),
            state = SwapState.AWAITING_TAKER_INFORMATION.asString
        )
        withContext(Dispatchers.Main) {
            swapTruthSource.swaps[swapID]?.state?.value = SwapState.AWAITING_TAKER_INFORMATION
        }
        // Since we are the taker of this swap, we should have it in persistent storage
        val swapInDatabase = databaseService.getSwap(id = swapIDString)
            ?: throw SwapServiceException("Could not find swap $swapID in persistent storage")
        val decoder = Base64.getDecoder()
        val makerInterfaceID = try {
            decoder.decode(swapInDatabase.makerInterfaceID)
        } catch (exception: Exception) {
            throw SwapServiceException("Unable to get maker interface ID for $swapID. makerInterfaceID string: " +
                    swapInDatabase.makerInterfaceID
            )
        }
        /*
        The user of this interface has taken the swap. Since taking a swap is not allowed unless we have a copy of the
        maker's public key, we should have said public key in storage.
         */
        val makerPublicKey = keyManagerService.getPublicKey(makerInterfaceID)
            ?: throw SwapServiceException("Could not find maker's public key for $swapID")
        val takerInterfaceID = try {
            decoder.decode(swapInDatabase.takerInterfaceID)
        } catch (exception: Exception) {
            throw SwapServiceException("Unable to get taker interface ID for $swapID. takerInterfaceID string: " +
                    swapInDatabase.takerInterfaceID)
        }
        /*
        Since the user of this interface has taken the swap, we should have a key pair for the swap in persistent
        storage.
         */
        val takerKeyPair = keyManagerService.getKeyPair(takerInterfaceID)
            ?: throw SwapServiceException("Could not find taker's (user's) key pair for $swapID")
        // TODO: get actual payment details once settlementMethodService is implemented
        val settlementMethodDetailsString = "TEMPORARY"
        logger.info("sendTakerInformationMessage: sending for $swapID")
        p2pService.sendTakerInformation(
            makerPublicKey = makerPublicKey,
            takerKeyPair = takerKeyPair,
            swapID = swapID,
            settlementMethodDetails = settlementMethodDetailsString
        )
        logger.info("sendTakerInformationMessage: sent for $swapID")
        databaseService.updateSwapState(
            swapID = swapIDString,
            chainID = chainID.toString(),
            state = SwapState.AWAITING_MAKER_INFORMATION.asString
        )
        withContext(Dispatchers.Main) {
            swapTruthSource.swaps[swapID]?.state?.value = SwapState.AWAITING_MAKER_INFORMATION
        }
    }

    /**
     * Attempts to fill a maker-as-seller [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap),
     * using the process described in the
     * [interface specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
     *
     * On the IO coroutine dispatcher, this ensures that the user is the maker and stablecoin seller of [swapToFill],
     * and that [swapToFill] can actually be filled. Then this approves token transfer for [swapToFill]'s
     * [Swap.takenSwapAmount] to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract, calls the
     * CommutoSwap contract's [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap)
     * function (via [blockchainService]), persistently updates the state of [swapToFill] to
     * [SwapState.FILL_SWAP_TRANSACTION_BROADCAST], and sets [swapToFill]'s [Swap.requiresFill] property to false.
     * Finally, on the main coroutine dispatcher, this updates [swapToFill]'s [Swap.state] value to
     * [SwapState.FILL_SWAP_TRANSACTION_BROADCAST].
     *
     * @param swapToFill The [Swap] that this function will fill.
     * @param afterPossibilityCheck A lambda that will be executed after this has ensured that the swap can be filled.
     * @param afterTransferApproval A lambda that will be executed after the token transfer approval to the
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol) contract is completed.
     *
     * @throws SwapServiceException if [swapToFill] is not a maker-as-seller swap and the user is not the maker, or if
     * the [swapToFill]'s state is not `awaitingFilling`
     */
    suspend fun fillSwap(
        swapToFill: Swap,
        afterPossibilityCheck: (suspend () -> Unit)? = null,
        afterTransferApproval: (suspend () -> Unit)? = null,
    ) {
        withContext(Dispatchers.IO) {
            logger.info("fillSwap: checking that ${swapToFill.id} can be filled")
            try {
                if (swapToFill.role != SwapRole.MAKER_AND_SELLER) {
                    throw SwapServiceException("Only Maker-As-Seller swaps can be filled")
                }
                if (swapToFill.state.value != SwapState.AWAITING_FILLING) {
                    throw SwapServiceException("This Swap cannot currently be filled")
                }
                afterPossibilityCheck?.invoke()
                logger.info("fillSwap: authorizing transfer for ${swapToFill.id}. Amount: " +
                        "${swapToFill.takenSwapAmount}")
                blockchainService.approveTokenTransferAsync(
                    tokenAddress = swapToFill.stablecoin,
                    destinationAddress = blockchainService.getCommutoSwapAddress(),
                    amount = swapToFill.takenSwapAmount,
                ).await()
                afterTransferApproval?.invoke()
                logger.info("fillSwap: filling ${swapToFill.id}")
                blockchainService.fillSwapAsync(
                    id = swapToFill.id
                ).await()
                logger.info("fillSwap: filled ${swapToFill.id}")
                val encoder = Base64.getEncoder()
                val swapIDB64String = encoder.encodeToString(swapToFill.id.asByteArray())
                databaseService.updateSwapState(
                    swapID = swapIDB64String,
                    chainID = swapToFill.chainID.toString(),
                    state = SwapState.FILL_SWAP_TRANSACTION_BROADCAST.asString,
                )
                databaseService.updateSwapRequiresFill(
                    swapID = swapIDB64String,
                    chainID = swapToFill.chainID.toString(),
                    requiresFill = false,
                )
                // This is not a MutableState property, so we can upgrade it from a background thread
                swapToFill.requiresFill = false
                withContext(Dispatchers.Main) {
                    swapToFill.state.value = SwapState.FILL_SWAP_TRANSACTION_BROADCAST
                }
            } catch (exception: Exception) {
                logger.error("fillSwap: encountered exception during call for ${swapToFill.id}", exception)
                throw exception
            }
        }
    }

    /**
     * Gets the on-chain [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified
     * swap ID, creates and persistently stores a new [Swap] with state [SwapState.AWAITING_TAKER_INFORMATION] using the
     * on chain swap, and maps [swapID] to the new [Swap] on the main coroutine dispatcher.
     */
    override suspend fun handleNewSwap(swapID: UUID, chainID: BigInteger) {
        logger.info("handleNewSwap: getting on-chain struct for $swapID")
        val swapOnChain = blockchainService.getSwap(id = swapID)
        if (swapOnChain == null) {
            logger.error("handleNewSwap: could not find $swapID on chain, throwing exception")
            throw SwapServiceException("Could not find $swapID on chain")
        }
        val direction: OfferDirection = when (swapOnChain.direction) {
            BigInteger.ZERO -> OfferDirection.BUY
            BigInteger.ONE -> OfferDirection.SELL
            else -> throw SwapServiceException("Swap $swapID has invalid direction: ${swapOnChain.direction}")
        }
        val swapRole = when (direction) {
            OfferDirection.BUY -> SwapRole.MAKER_AND_BUYER
            OfferDirection.SELL -> SwapRole.MAKER_AND_SELLER
        }
        val newSwap = Swap(
            isCreated = swapOnChain.isCreated,
            requiresFill = swapOnChain.requiresFill,
            id = swapID,
            maker = swapOnChain.maker,
            makerInterfaceID = swapOnChain.makerInterfaceID,
            taker = swapOnChain.taker,
            takerInterfaceID = swapOnChain.takerInterfaceID,
            stablecoin = swapOnChain.stablecoin,
            amountLowerBound = swapOnChain.amountLowerBound,
            amountUpperBound = swapOnChain.amountUpperBound,
            securityDepositAmount = swapOnChain.securityDepositAmount,
            takenSwapAmount = swapOnChain.takenSwapAmount,
            serviceFeeAmount = swapOnChain.serviceFeeAmount,
            serviceFeeRate = swapOnChain.serviceFeeRate,
            direction = direction,
            onChainSettlementMethod = swapOnChain.settlementMethod,
            protocolVersion = swapOnChain.protocolVersion,
            isPaymentSent = swapOnChain.isPaymentSent,
            isPaymentReceived = swapOnChain.isPaymentReceived,
            hasBuyerClosed = swapOnChain.hasBuyerClosed,
            hasSellerClosed = swapOnChain.hasSellerClosed,
            onChainDisputeRaiser = swapOnChain.disputeRaiser,
            chainID = swapOnChain.chainID,
            state = SwapState.AWAITING_TAKER_INFORMATION,
            role = swapRole,
        )
        // TODO: check for taker information once SettlementMethodService is implemented
        logger.info("handleNewSwap: persistently storing $swapID")
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = if (newSwap.isCreated) 1L else 0L,
            requiresFill = if (newSwap.requiresFill) 1L else 0L,
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
            isPaymentSent = if (newSwap.isPaymentSent) 1L else 0L,
            isPaymentReceived = if (newSwap.isPaymentReceived) 1L else 0L,
            hasBuyerClosed = if (newSwap.hasBuyerClosed) 1L else 0L,
            hasSellerClosed = if (newSwap.hasSellerClosed) 1L else 0L,
            disputeRaiser = newSwap.onChainDisputeRaiser.toString(),
            chainID = newSwap.chainID.toString(),
            state = newSwap.state.value.asString,
            role = newSwap.role.asString,
        )
        databaseService.storeSwap(swapForDatabase)
        // Add new Swap to swapTruthSource
        withContext(Dispatchers.Main) {
            swapTruthSource.addSwap(newSwap)
        }
        logger.info("handleNewSwap: successfully handled $swapID")
    }

    /**
     * The function called by [P2PService] to notify [SwapService] of a [TakerInformationMessage].
     *
     * Once notified, this checks that there exists in [swapTruthSource] a [Swap] with the ID specified in [message]. If
     * there is, this checks that the interface ID of the public key contained in [message] is equal to the interface ID
     * of the swap taker. If it is, this persistently stores the public key in [message], securely persistently stores
     * the payment information in [message], updates the state of the swap to [SwapState.AWAITING_MAKER_INFORMATION]
     * (both persistently and in [swapTruthSource] on the main coroutine dispatcher), and creates and sends a Maker
     * Information Message. Then, if the swap is a maker-as-seller swap, this updates the state of the swap to
     * [SwapState.AWAITING_FILLING]. Otherwise, the swap is a maker-as-buyer swap, and this updates the state of the
     * swap to [SwapState.AWAITING_PAYMENT_SENT]. The state is updated both persistently and in [swapTruthSource].
     *
     * @param message The [TakerInformationMessage] to handle.
     *
     * @throws SwapServiceException If [keyManagerService] does not have the key pair with the interface ID specified in
     * [message].
     */
    override suspend fun handleTakerInformationMessage(message: TakerInformationMessage) {
        logger.info("handleTakerInformationMessage: handling for ${message.swapID}")
        val swap = swapTruthSource.swaps[message.swapID]
        if (swap != null) {
            if (message.publicKey.interfaceId.contentEquals(swap.takerInterfaceID)) {
                val encoder = Base64.getEncoder()
                logger.info("handleTakerInformationMessage: persistently storing public key " +
                        "${encoder.encodeToString(message.publicKey.interfaceId)} for ${message.swapID}")
                keyManagerService.storePublicKey(message.publicKey)
                // TODO: securely store taker settlement method information once SettlementMethodService is implemented
                logger.info("handleTakerInformationMessage: updating ${message.swapID} to " +
                        SwapState.AWAITING_MAKER_INFORMATION.asString)
                val swapIDB64String = encoder.encodeToString(message.swapID.asByteArray())
                databaseService.updateSwapState(
                    swapID = swapIDB64String,
                    chainID = swap.chainID.toString(),
                    state = SwapState.AWAITING_MAKER_INFORMATION.asString,
                )
                withContext(Dispatchers.Main) {
                    swap.state.value = SwapState.AWAITING_MAKER_INFORMATION
                }
                // TODO: get actual settlement method details once SettlementMethodService is implemented
                val makerKeyPair = keyManagerService.getKeyPair(swap.makerInterfaceID)
                    ?: throw SwapServiceException("Could not find key pair for ${message.swapID} while handling " +
                            "Taker Information Message")
                val settlementMethodDetailsString = "TEMPORARY"
                logger.info("handleTakerInformationMessage: sending for ${message.swapID}")
                p2pService.sendMakerInformation(
                    takerPublicKey = message.publicKey,
                    makerKeyPair = makerKeyPair,
                    swapID = message.swapID,
                    settlementMethodDetails = settlementMethodDetailsString
                )
                logger.info("handleTakerInformationMessage: sent for ${message.swapID}")
                when (swap.direction) {
                    OfferDirection.BUY -> {
                        logger.info("handleTakerInformationMessage: updating state of BUY swap " +
                                "${message.swapID} to ${SwapState.AWAITING_PAYMENT_SENT.asString}")
                        databaseService.updateSwapState(
                            swapID = swapIDB64String,
                            chainID = swap.chainID.toString(),
                            state = SwapState.AWAITING_PAYMENT_SENT.asString,
                        )
                        withContext(Dispatchers.Main) {
                            swap.state.value = SwapState.AWAITING_PAYMENT_SENT
                        }
                    }
                    OfferDirection.SELL -> {
                        logger.info("handleTakerInformationMessage: updating state of SELL swap " +
                                "${message.swapID} to ${SwapState.AWAITING_FILLING.asString}")
                        databaseService.updateSwapState(
                            swapID = swapIDB64String,
                            chainID = swap.chainID.toString(),
                            state = SwapState.AWAITING_FILLING.asString,
                        )
                        withContext(Dispatchers.Main) {
                            swap.state.value = SwapState.AWAITING_FILLING
                        }
                    }
                }
            } else {
                logger.warn("handleTakerInformationMessage: got message for ${message.swapID} that was not " +
                        "sent by swap taker")
            }
        } else {
            logger.warn("handleTakerInformationMessage: got message for ${message.swapID} which was not found " +
                    "in swapTruthSource")
        }
    }

    /**
     * The function called by [P2PService] to notify [SwapService] of a [MakerInformationMessage].
     *
     * Once notified, this checks that there exists in [swapTruthSource] a [Swap] with the ID specified in [message]. If
     * there is, this checks that the interface ID of the message sender is equal to the interface ID of the swap maker
     * and that the interface ID of the message's intended recipient is equal to the interface ID of the swap taker. If
     * they are, this securely persistently stores the settlement method information in [message]. Then, if this swap is
     * a maker-as-buyer swap, this updates the state of the swap to [SwapState.AWAITING_PAYMENT_SENT]. Otherwise, the
     * swap is a maker-as-seller swap, and this updates the state of the swap to [SwapState.AWAITING_FILLING]. The state
     * is updated both persistently and in [swapTruthSource].
     *
     * @param message The [MakerInformationMessage] to handle.
     * @param senderInterfaceID The interface ID of the message's sender.
     * @param recipientInterfaceID The interface ID of the message's intended recipient.
     */
    override suspend fun handleMakerInformationMessage(
        message: MakerInformationMessage,
        senderInterfaceID: ByteArray,
        recipientInterfaceID: ByteArray
    ) {
        logger.info("handleMakerInformationMessage: handling for ${message.swapID}")
        val swap = swapTruthSource.swaps[message.swapID]
        if (swap != null) {
            if (!senderInterfaceID.contentEquals(swap.makerInterfaceID)) {
                logger.warn("handleMakerInformationMessage: got message for ${message.swapID} that was not " +
                        "sent by swap maker")
                return
            }
            val encoder = Base64.getEncoder()
            if (!recipientInterfaceID.contentEquals(swap.takerInterfaceID)) {
                logger.warn("handleMakerInformationMessage: got message for ${message.swapID} with recipient " +
                        "interface ID ${encoder.encodeToString(recipientInterfaceID)} that doesn't match taker " +
                        "interface ID ${encoder.encodeToString(swap.takerInterfaceID)}")
                return
            }
            // TODO: securely store maker settlement method information once SettlementMethodService is implemented
            when (swap.direction) {
                OfferDirection.BUY -> {
                    logger.info("handleMakerInformationMessage: updating state of BUY swap ${message.swapID} " +
                            "to ${SwapState.AWAITING_PAYMENT_SENT.asString}")
                    databaseService.updateSwapState(
                        swapID = encoder.encodeToString(message.swapID.asByteArray()),
                        chainID = swap.chainID.toString(),
                        state = SwapState.AWAITING_PAYMENT_SENT.asString,
                    )
                    withContext(Dispatchers.Main) {
                        swap.state.value = SwapState.AWAITING_PAYMENT_SENT
                    }
                }
                OfferDirection.SELL -> {
                    logger.info("handleMakerInformationMessage: updating state of SELL swap ${message.swapID} " +
                            "to ${SwapState.AWAITING_FILLING.asString}")
                    databaseService.updateSwapState(
                        swapID = encoder.encodeToString(message.swapID.asByteArray()),
                        chainID = swap.chainID.toString(),
                        state = SwapState.AWAITING_FILLING.asString,
                    )
                    withContext(Dispatchers.Main) {
                        swap.state.value = SwapState.AWAITING_FILLING
                    }
                }
            }
        } else {
            logger.warn("handleMakerInformationMessage: got message for ${message.swapID} which was not found " +
                    "in swapTruthSource")
        }
    }

    /**
     * The function called by [BlockchainService] to notify [SwapService] of a [SwapFilledEvent].
     *
     * Once notified, [SwapService] checks in [swapTruthSource] for a [Swap` with the ID specified in [event]. If it
     * finds such a swap, it ensures that the chain ID of the swap and the chain ID of [event] match. Then, if the role
     * of the swap is [SwapRole.MAKER_AND_BUYER] or [SwapRole.TAKER_AND_SELLER], it logs a warning, since
     * [SwapFilledEvent]s should not be emitted for such swaps. Otherwise, the role is [SwapRole.MAKER_AND_SELLER] or
     * [SwapRole.TAKER_AND_BUYER], and this persistently sets the state of the swap to [SwapState.AWAITING_PAYMENT_SENT]
     * and requiresFill to `false`, and then does the same to the [Swap] object on the main coroutine dispatcher.
     *
     * @param event The [SwapFilledEvent] of which [SwapService] is being notified.
     *
     * @throws SwapServiceException if the chain ID of [event] and the [Swap] with the ID specified in [event] do not
     * match.
     */
    override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {
        logger.info("handleSwapFilledEvent: handing for ${event.swapID}")
        swapTruthSource.swaps[event.swapID]?.let { swap ->
            // Executed if we have a Swap with the ID specified in event
            if (swap.chainID != event.chainID) {
                throw SwapServiceException("Chain ID of SwapFilledEvent did not match chain ID of Swap in " +
                        "handleSwapFilledEvent call. SwapFilledEvent.chainID: ${event.chainID}, Swap.chainID: " +
                        "${swap.chainID}, SwapFilledEvent.id: ${event.swapID}")
            }
            /*
            At this point, we have ensured that the chain ID of the event and the swap match, so we can proceed safely
             */
            when (swap.role) {
                SwapRole.MAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> {
                    /*
                    If we are the maker of and buyer in or taker of and seller in the swap, no SwapFilled event should
                    be emitted for that swap
                     */
                    logger.warn("handleSwapFilledEvent: unexpectedly got event for ${event.swapID} with role " +
                            swap.role.asString)
                }
                SwapRole.MAKER_AND_SELLER, SwapRole.TAKER_AND_BUYER -> {
                    logger.info("handleSwapFilledEvent: handing for ${event.swapID} as ${swap.role.asString}")
                    /*
                    We have gotten confirmation that the fillSwap transaction has been confirmed, so we update the state
                    of the swap to indicate that we are waiting for the buyer to send traditional currency payment
                     */
                    val swapIDB64String = Base64.getEncoder().encodeToString(event.swapID.asByteArray())
                    databaseService.updateSwapRequiresFill(
                        swapID = swapIDB64String,
                        chainID = event.chainID.toString(),
                        requiresFill = false
                    )
                    databaseService.updateSwapState(
                        swapID = swapIDB64String,
                        chainID = event.chainID.toString(),
                        state = SwapState.AWAITING_PAYMENT_SENT.asString
                    )
                    swap.requiresFill = false
                    withContext(Dispatchers.Main) {
                        swap.state.value =  SwapState.AWAITING_PAYMENT_SENT
                    }
                }
            }
        } ?: run {
            // Executed if we do not have a Swap with the ID specified in event
            logger.info("handleSwapFilledEvent: ${event.swapID} not made or taken by user")
            return
        }
    }

}