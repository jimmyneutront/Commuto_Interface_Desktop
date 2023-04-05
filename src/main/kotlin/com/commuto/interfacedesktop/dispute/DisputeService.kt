package com.commuto.interfacedesktop.dispute

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionType
import com.commuto.interfacedesktop.blockchain.events.commutoswap.DisputeRaisedEvent
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.dispute.validation.validateSwapForRaisingDispute
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.p2p.DisputeMessageNotifiable
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncementAsUserForDispute
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.swap.SwapRole
import com.commuto.interfacedesktop.swap.SwapTruthSource
import com.commuto.interfacedesktop.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Dispute service. It is responsible for processing and organizing dispute-related data.
 *
 * @property databaseService The [DatabaseService] that this [DisputeService] uses for persistent storage.
 * @property keyManagerService The [KeyManagerService] that this [DisputeService] uses for managing keys.
 * @property swapTruthSource The [SwapTruthSource] containing all [Swap]s.
 * @property disputeTruthSource The [DisputeTruthSource] containing all [SwapAndDispute]s.
 * @property blockchainService The [BlockchainService] that this uses to interact with the blockchain.
 * @property p2pService The [P2PService] that this uses for interacting with the peer-to-peer network.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 */
@Singleton
class DisputeService @Inject constructor(
    private val databaseService: DatabaseService,
    private val keyManagerService: KeyManagerService,
): DisputeNotifiable, DisputeMessageNotifiable {

    private lateinit var swapTruthSource: SwapTruthSource

    private lateinit var disputeTruthSource: DisputeTruthSource

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
     * Used to set the [disputeTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [disputeTruthSource] property, which cannot be null.
     */
    fun setDisputeTruthSource(newTruthSource: DisputeTruthSource) {
        check(!::disputeTruthSource.isInitialized) {
            "disputeTruthSource is already initialized"
        }
        disputeTruthSource = newTruthSource
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
     * Attempts to create a [RawTransaction] that will raise a dispute for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) involving the user of this
     * interface.
     *
     * This calls [validateSwapForRaisingDispute], then gets all active dispute agent addresses, randomly selects three
     * of them, and then passes the selected addresses along with the ID and chain ID of [swap] to
     * [BlockchainService.createRaiseDisputeTransaction].
     *
     * @param swap The [Swap] for which a dispute will be raised.
     *
     * @return A [Pair], the first element of which is a [RawTransaction] capable of raising a dispute for [swap], and
     * the second element of which is a [Triple] of [String]s that are the selected dispute agents.
     */
    suspend fun createRaiseDisputeTransaction(swap: Swap): Pair<RawTransaction, Triple<String, String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("createRaiseDisputeTransaction: creating for ${swap.id}")
                validateSwapForRaisingDispute(swap = swap)
                val activeDisputeAgents = blockchainService.getActiveDisputeAgentsAsync().await()
                val disputeAgentZero = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentZero)
                val disputeAgentOne = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentOne)
                val disputeAgentTwo = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentTwo)
                Pair(
                    blockchainService.createRaiseDisputeTransaction(
                        swapID = swap.id,
                        chainID = swap.chainID,
                        disputeAgent0 = disputeAgentZero,
                        disputeAgent1 = disputeAgentOne,
                        disputeAgent2 = disputeAgentTwo,
                    ),
                    Triple(
                        disputeAgentZero,
                        disputeAgentOne,
                        disputeAgentTwo
                    )
                )
            } catch (exception: Exception) {
                logger.error("createRaiseDisputeTransaction: encountered exception while creating for ${swap.id}", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to raise a dispute for a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap)
     * involving the user of this interface.
     *
     * @param swap The [Swap] for which to raise a dispute.
     * @param disputeRaisingTransaction An optional [RawTransaction] that can raise a dispute for [swap].
     * @param disputeAgents An optional [Triple] of [String]s, containing the addresses of the dispute agents with which
     * [disputeRaisingTransaction] was created.
     *
     * @throws [DisputeServiceException] if [disputeAgents] is `null` or if [disputeRaisingTransaction] is `null` or if
     * the data of [disputeRaisingTransaction] does not match that of the transaction this function creates using [swap]
     * and [disputeAgents].
     */
    suspend fun raiseDispute(
        swap: Swap,
        disputeRaisingTransaction: RawTransaction?,
        disputeAgents: Triple<String, String, String>?
    ) {
        withContext(Dispatchers.IO) {
            val encoder = Base64.getEncoder()
            try  {
                logger.info("raiseDispute: raising for ${swap.id}")
                validateSwapForRaisingDispute(swap = swap)
                if (disputeAgents == null) {
                    throw DisputeServiceException(message = "disputeAgents was null during raiseDispute call for " +
                            "${swap.id}")
                }
                logger.info("raiseDispute: recreating RawTransaction to dispute ${swap.id} to ensure " +
                        "disputeRaisingTransaction was created with the contents of swap")
                val recreatedTransaction = blockchainService.createRaiseDisputeTransaction(
                    swapID = swap.id,
                    chainID = swap.chainID,
                    disputeAgent0 = disputeAgents.first,
                    disputeAgent1 = disputeAgents.second,
                    disputeAgent2 = disputeAgents.third
                )
                if (disputeRaisingTransaction == null) {
                    throw DisputeServiceException(message = "Transaction was null during raiseDispute call for " +
                            "${swap.id}")
                }
                if (recreatedTransaction.data != disputeRaisingTransaction.data) {
                    throw DisputeServiceException(message = "Data for disputeRaisingTransaction did not match that of " +
                            "transaction created with swap ${swap.id}")
                }
                logger.info("raiseDispute: signing transaction for ${swap.id}")
                val signedTransactionData = blockchainService.signTransaction(
                    transaction = disputeRaisingTransaction,
                    chainID = swap.chainID,
                )
                val signedTransactionHex = Numeric.toHexString(signedTransactionData)
                val blockchainTransactionForRaisingDispute = BlockchainTransaction(
                    transaction = disputeRaisingTransaction,
                    transactionHash = Hash.sha3(signedTransactionHex),
                    latestBlockNumberAtCreation = blockchainService.newestBlockNum,
                    type = BlockchainTransactionType.RAISE_DISPUTE
                )
                val dateString = DateFormatter.createDateString(blockchainTransactionForRaisingDispute.timeOfCreation)
                logger.info("raiseDispute: persistently storing dispute raising data for ${swap.id}, including tx " +
                        "hash ${blockchainTransactionForRaisingDispute.transactionHash}")
                databaseService.updateRaisingDisputeData(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    transactionHash = blockchainTransactionForRaisingDispute.transactionHash,
                    creationTime = dateString,
                    blockNumber = blockchainTransactionForRaisingDispute.latestBlockNumberAtCreation.toLong()
                )
                logger.info("raiseDispute: persistently  updating raisingDisputeState for ${swap.id} to " +
                        "${RaisingDisputeState.SENDING_TRANSACTION}")
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.SENDING_TRANSACTION.asString,
                )
                logger.info("raiseDispute: updating raisingDisputeState to ${RaisingDisputeState.SENDING_TRANSACTION} " +
                        "and storing tx ${blockchainTransactionForRaisingDispute.transactionHash} in ${swap.id}")
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeTransaction = blockchainTransactionForRaisingDispute
                    swap.raisingDisputeState.value = RaisingDisputeState.SENDING_TRANSACTION
                }
                logger.info("raiseDispute: sending ${blockchainTransactionForRaisingDispute.transactionHash} for " +
                        "${swap.id}")
                blockchainService.sendTransaction(
                    transaction = blockchainTransactionForRaisingDispute,
                    signedRawTransactionDataAsHex = signedTransactionHex,
                    chainID = swap.chainID
                )
                logger.info("raiseDispute: persistently updating raisingDisputeState of ${swap.id} to " +
                        "${RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION}")
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION.asString,
                )
                logger.info("raiseDispute: updating raisingDisputeState to " +
                        "${RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION} for ${swap.id}")
                withContext(Dispatchers.Main){
                    swap.raisingDisputeState.value = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION
                }
            } catch (exception: Exception) {
                logger.error("raiseDispute: encountered exception while raising dispute for ${swap.id}, setting " +
                        "raisingDisputeState to ${RaisingDisputeState.EXCEPTION}", exception)
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.EXCEPTION.asString,
                )
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeException = exception
                    swap.raisingDisputeState.value = RaisingDisputeState.EXCEPTION
                }
            }
        }
    }

    /**
     * The function called by [BlockchainService] in order to notify [DisputeService] that a monitored dispute-related
     * [BlockchainTransaction] has failed (either has been confirmed and failed, or has been dropped.)
     *
     * @param transaction The [BlockchainTransaction] wrapping the on-chain transaction that has failed.
     * @param exception A [BlockchainTransactionException] describing why the on-chain transaction has failed.
     *
     * @throws [DisputeServiceException] if this is passed a non-dispute-related [BlockchainTransaction].
     */
    override suspend fun handleFailedTransaction(
        transaction: BlockchainTransaction,
        exception: BlockchainTransactionException
    ) {
        logger.warn("handleFailedTransaction: handling ${transaction.transactionHash} of type " +
                "${transaction.type.asString} with exception ${exception.message}")
        val encoder = Base64.getEncoder()
        when (transaction.type) {
            BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER, BlockchainTransactionType.OPEN_OFFER,
            BlockchainTransactionType.CANCEL_OFFER, BlockchainTransactionType.EDIT_OFFER,
            BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER, BlockchainTransactionType.TAKE_OFFER,
            BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP, BlockchainTransactionType.FILL_SWAP,
            BlockchainTransactionType.REPORT_PAYMENT_SENT, BlockchainTransactionType.REPORT_PAYMENT_RECEIVED,
            BlockchainTransactionType.CLOSE_SWAP -> {
                throw DisputeServiceException(message = "handleFailedTransaction: received a non-swap-related " +
                        "transaction ${transaction.transactionHash}")
            }
            BlockchainTransactionType.RAISE_DISPUTE -> {
                val swap = swapTruthSource.swaps.firstNotNullOfOrNull { uuidSwapEntry ->
                    if (uuidSwapEntry.value.raisingDisputeTransaction?.transactionHash
                            .equals(transaction.transactionHash)) {
                        uuidSwapEntry.value
                    } else {
                        null
                    }
                }
                if (swap != null) {
                    logger.warn("handleFailedTransaction: found swap ${swap.id} on ${swap.chainID} with raising " +
                            "dispute transaction ${transaction.transactionHash}, updating raisingDisputeState to " +
                            "${RaisingDisputeState.EXCEPTION.asString} in persistent storage")
                    databaseService.updateRaisingDisputeState(
                        swapID = encoder.encodeToString(swap.id.asByteArray()),
                        chainID = swap.chainID.toString(),
                        state = RaisingDisputeState.EXCEPTION.asString,
                    )
                    logger.warn("handleFailedTransaction: setting raisingDisputeException and updating " +
                            "raisingDisputeState to ${RaisingDisputeState.EXCEPTION.asString} for for ${swap.id}")
                    withContext(Dispatchers.Main) {
                        swap.raisingDisputeException = exception
                        swap.raisingDisputeState.value = RaisingDisputeState.EXCEPTION
                    }
                } else {
                    logger.warn("handleFailedTransaction: swap with dispute raising transaction ${transaction
                        .transactionHash} not found in swapTruthSource")
                }
            }
        }
    }

    /**
     * The function called by [BlockchainService] to notify [DisputeService] of a [DisputeRaisedEvent].
     *
     * @param event The [DisputeRaisedEvent] of which [DisputeService] is being notified.
     */
    override suspend fun handleDisputeRaisedEvent(event: DisputeRaisedEvent) {
        logger.info("handleDisputeRaisedEvent: handling event for ${event.swapID} on ${event.chainID}")
        val swapOnChain = blockchainService.getSwap(id = event.swapID)
            ?: throw DisputeServiceException(message = "Could not find on-chain swap ${event.swapID} on ${event
                .chainID}")
        if (event.disputeAgent0.lowercase() == blockchainService.getAddress().lowercase()) {
            logger.info("handleDisputeRaisedEvent: user is first dispute agent for ${event.swapID} on ${event.chainID}")
            val disputeOnChain = blockchainService.getDisputeAsync(id = event.swapID).await()
            val swapAndDispute = SwapAndDispute(
                isCreated = true,
                requiresFill = swapOnChain.requiresFill,
                id = event.swapID,
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
                direction = when (swapOnChain.direction) {
                    BigInteger.ZERO -> OfferDirection.BUY
                    BigInteger.ONE -> OfferDirection.SELL
                    else -> throw DisputeServiceException("Swap ${event.swapID} has invalid direction: ${swapOnChain
                        .direction}")
                },
                onChainSettlementMethod = swapOnChain.settlementMethod,
                protocolVersion = swapOnChain.protocolVersion,
                isPaymentSent = swapOnChain.isPaymentSent,
                isPaymentReceived = swapOnChain.isPaymentReceived,
                hasBuyerClosed = swapOnChain.hasBuyerClosed,
                hasSellerClosed = swapOnChain.hasSellerClosed,
                onChainDisputeRaiser = swapOnChain.disputeRaiser,
                chainID = swapOnChain.chainID,
                disputeRaisedBlockNumber = disputeOnChain.disputeRaisedBlockNum,
                disputeAgent0 = disputeOnChain.disputeAgent0,
                disputeAgent1 = disputeOnChain.disputeAgent1,
                disputeAgent2 = disputeOnChain.disputeAgent2,
                hasDisputeAgent0Proposed = disputeOnChain.hasDA0Proposed,
                disputeAgent0MakerPayout = disputeOnChain.dA0MakerPayout,
                disputeAgent0TakerPayout = disputeOnChain.dA0TakerPayout,
                disputeAgent0ConfiscationPayout = disputeOnChain.dA0ConfiscationPayout,
                hasDisputeAgent1Proposed = disputeOnChain.hasDA1Proposed,
                disputeAgent1MakerPayout = disputeOnChain.dA1MakerPayout,
                disputeAgent1TakerPayout = disputeOnChain.dA1TakerPayout,
                disputeAgent1ConfiscationPayout = disputeOnChain.dA1ConfiscationPayout,
                hasDisputeAgent2Proposed = disputeOnChain.hasDA2Proposed,
                disputeAgent2MakerPayout = disputeOnChain.dA2MakerPayout,
                disputeAgent2TakerPayout = disputeOnChain.dA2TakerPayout,
                disputeAgent2ConfiscationPayout = disputeOnChain.dA2ConfiscationPayout,
                onChainMatchingProposals = disputeOnChain.matchingProposals,
                makerReaction = disputeOnChain.makerReaction,
                takerReaction = disputeOnChain.takerReaction,
                onChainState = disputeOnChain.state,
                hasMakerPaidOut = disputeOnChain.hasMakerPaidOut,
                hasTakerPaidOut = disputeOnChain.hasTakerPaidOut,
                totalWithoutSpentServiceFees = disputeOnChain.totalWithoutSpentServiceFees,
                role = DisputeRole.DISPUTE_AGENT_0,
            )
            logger.info("handleDisputeRaisedEvent: persistently storing ${event.swapID} on ${event.chainID} for " +
                    "which user is first dispute agent")
            databaseService.storeSwapAndDispute(
                swapAndDispute = swapAndDispute.toDatabaseSwapAndDispute()
            )
            logger.info("handleDisputeRaisedEvent: generating key pair for user/first dispute agent for ${event
                .swapID} on ${event.chainID}")
            val keyPair = keyManagerService.generateKeyPair(storeResult = true)
            val encoder = Base64.getEncoder()
            logger.info("handleDisputeRaisedEvent: generated key pair with interface id ${encoder.encodeToString(keyPair
                .interfaceId)} for user/first dispute agent for ${event.swapID} on ${event.chainID}, associating " +
                    "with swapAndDispute")
            swapAndDispute.disputeAgent0InterfaceID = keyPair.interfaceId
            databaseService.updateSwapAndDisputeAgent0InterfaceID(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                interfaceID = encoder.encodeToString(swapAndDispute.disputeAgent0InterfaceID)
            )
            logger.info("handleDisputeRaisedEvent: announcing key ${encoder.encodeToString(keyPair.interfaceId)} for " +
                    "user/first dispute agent for ${event.swapID} on ${event.chainID}")
            p2pService.announcePublicKeyAsAgentForDispute(
                keyPair = keyPair,
                swapId = swapAndDispute.id,
                role = swapAndDispute.role,
                ethereumKeyPair = blockchainService.getCredentials(),
            )
            logger.info("handleDisputeRaisedEvent: updating state of ${event.swapID} on ${event.chainID} to " +
                    DisputeStateAsAgent.SENT_DISPUTE_AGENT_0_PKA.asString
            )
            databaseService.updateSwapAndDisputeState(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                state = DisputeStateAsAgent.SENT_DISPUTE_AGENT_0_PKA.asString
            )
            withContext(Dispatchers.Main) {
                swapAndDispute.state.value = DisputeStateAsAgent.SENT_DISPUTE_AGENT_0_PKA
            }
            logger.info("handleDisputeRaisedEvent: creating and storing symmetric keys for ${event.swapID} on ${event
                .chainID}")
            val makerCommunicationKey = SymmetricKey()
            val takerCommunicationKey = SymmetricKey()
            val disputeAgentCommunicationKey = SymmetricKey()
            databaseService.updateSwapAndDisputeMakerCommunicationKey(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                key = encoder.encodeToString(makerCommunicationKey.keyBytes),
            )
            databaseService.updateSwapAndDisputeTakerCommunicationKey(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                key = encoder.encodeToString(takerCommunicationKey.keyBytes),
            )
            databaseService.updateSwapAndDisputeAgentCommunicationKey(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                key = encoder.encodeToString(disputeAgentCommunicationKey.keyBytes),
            )
            swapAndDispute.makerCommunicationKey = makerCommunicationKey
            swapAndDispute.takerCommunicationKey = makerCommunicationKey
            swapAndDispute.disputeAgentCommunicationKey = disputeAgentCommunicationKey
            logger.info("handleDisputeRaisedEvent: updating state of ${event.swapID} on ${event.chainID} to " +
                    DisputeStateAsAgent.CREATED_COMMUNICATION_KEYS.asString
            )
            databaseService.updateSwapAndDisputeState(
                id = swapAndDispute.id.toString(),
                chainID = swapAndDispute.chainID.toString(),
                state = DisputeStateAsAgent.CREATED_COMMUNICATION_KEYS.asString
            )
            withContext(Dispatchers.Main) {
                swapAndDispute.state.value = DisputeStateAsAgent.CREATED_COMMUNICATION_KEYS
            }
            logger.info("handleDisputeRaisedEvent: storing ${event.swapID} on ${event.chainID} in disputeTruthSource")
            withContext(Dispatchers.Main) {
                disputeTruthSource.addSwapAndDispute(swapAndDispute)
            }
            logger.info("handleDisputeRaisedEvent: successfully handled ${event.swapID} on ${event.chainID}")
        } else {
            val swap = swapTruthSource.swaps[event.swapID]
            if (swap == null) {
                logger.info("handleDisputeRaisedEvent: got event for ${event.swapID} not found in swapTruthSource")
                return
            }
            if (swap.chainID != event.chainID) {
                logger.warn("handleDisputeRaisedEvent: chain ID ${event.chainID} does not match chain ID of swap " +
                        "${swap.id}: ${swap.chainID}")
                return
            }
            val encoder = Base64.getEncoder()
            logger.info("handleDisputeRaisedEvent: user is swapper for ${swap.id}, retrieving key pair")
            val interfaceID = when (swap.role) {
                SwapRole.MAKER_AND_BUYER, SwapRole.MAKER_AND_SELLER -> swap.makerInterfaceID
                SwapRole.TAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> swap.takerInterfaceID
            }
            val keyPair = keyManagerService.getKeyPair(interfaceID) ?: throw DisputeServiceException("Could not " +
                    "find user's public key for ${swap.id}")
            logger.info("handleDisputeRaisedEvent: announcing public key for ${swap.id}")
            p2pService.announcePublicKeyAsUserForDispute(
                keyPair = keyPair
            )
            logger.info("handleDisputeRaisedEvent: updating dispute state of ${swap.id} to " +
                    "${DisputeState.SENT_PKA.asString} in persistent storage")
            databaseService.updateSwapDisputeState(
                swapID = encoder.encodeToString(swap.id.asByteArray()),
                chainID = swap.chainID.toString(),
                state = DisputeState.SENT_PKA.asString
            )
            logger.info("handleDisputeRaisedEvent: updating dispute state of ${swap.id} to ${DisputeState.SENT_PKA
                .asString}")
            withContext(Dispatchers.Main) {
                swap.disputeState.value = DisputeState.SENT_PKA
            }
            logger.info("handleDisputeRaisedEvent: checking dispute role for ${swap.id}")
            var mustUpdateDisputeRaisedTransaction = false
            if (
                (swapOnChain.disputeRaiser == BigInteger.ONE &&
                        (swap.role == SwapRole.MAKER_AND_BUYER || swap.role == SwapRole.MAKER_AND_SELLER)) ||
                (swapOnChain.disputeRaiser == BigInteger.valueOf(2L) &&
                        (swap.role == SwapRole.TAKER_AND_BUYER || swap.role == SwapRole.TAKER_AND_SELLER))
                ) {
                val raisingDisputeTransaction = swap.raisingDisputeTransaction
                if (raisingDisputeTransaction != null) {
                    if (raisingDisputeTransaction.transactionHash == event.transactionHash) {
                        logger.info("handleDisputeRaisedEvent: tx hash ${event.transactionHash} of event matches " +
                                "that for swap ${swap.id}: ${raisingDisputeTransaction.transactionHash}")
                    } else {
                        logger.warn("handleDisputeRaisedEvent: tx hash ${event.transactionHash} of event does not " +
                                "match that for swap ${swap.id}: ${raisingDisputeTransaction.transactionHash}, must " +
                                "update with new transaction hash")
                        mustUpdateDisputeRaisedTransaction = true
                    }
                } else {
                    logger.warn("handleDisputeRaisedEvent: swap ${swap.id} for which user is dispute raiser has no " +
                            "dispute raising transaction, must update with transaction hash ${event.transactionHash}")
                }
                logger.info("handleDisputeRaisedEvent: user is dispute raiser for ${swap.id} so persistently " +
                        "updating raisingDisputeState to ${RaisingDisputeState.COMPLETED.asString}")
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.COMPLETED.asString,
                )
                logger.info("handleDisputeRaisedEvent: updating raisingDisputeState to ${RaisingDisputeState.COMPLETED
                    .asString}")
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeState.value = RaisingDisputeState.COMPLETED
                }
            } else {
                logger.info("handleDisputeRaisedEvent: user is not dispute raiser for ${swap.id}, updating with " +
                        "transaction hash ${event.transactionHash}")
                mustUpdateDisputeRaisedTransaction = true
            }
            if (mustUpdateDisputeRaisedTransaction) {
                val raisingDisputeTransaction = BlockchainTransaction(
                    transactionHash = event.transactionHash,
                    timeOfCreation = Date(),
                    latestBlockNumberAtCreation = BigInteger.ZERO,
                    type = BlockchainTransactionType.RAISE_DISPUTE,
                )
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeTransaction = raisingDisputeTransaction
                }
                logger.warn("handleDisputeRaisedEvent: persistently storing tx data, including hash ${event
                    .transactionHash} for ${event.swapID}")
                val dateString = DateFormatter.createDateString(raisingDisputeTransaction.timeOfCreation)
                databaseService.updateRaisingDisputeData(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = event.chainID.toString(),
                    transactionHash = raisingDisputeTransaction.transactionHash,
                    creationTime = dateString,
                    blockNumber = raisingDisputeTransaction.latestBlockNumberAtCreation.toLong()
                )
            }
        }
    }

    override suspend fun handlePublicKeyAnnouncementAsUserForDispute(message: PublicKeyAnnouncementAsUserForDispute) {}

}