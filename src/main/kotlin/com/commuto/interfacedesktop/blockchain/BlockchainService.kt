package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.structs.OfferStruct
import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.OfferService
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.*
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainIdLong
import java.math.BigInteger
import java.net.ConnectException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Blockchain Service. It is responsible for listening to the blockchain and detecting the
 * emission of events by parsing transaction receipts, and then passing relevant detected events to
 * other services as necessary.
 *
 * @constructor Creates a new [BlockchainService] instance with the specified
 * [BlockchainExceptionNotifiable], [OfferNotifiable], [Web3j] instance and CommutoSwap contract
 * address.
 *
 * @property exceptionHandler An object to which [BlockchainService] will pass exceptions when they
 * occur.
 * @property offerService: An object to which [BlockchainService] will pass offer-related events
 * when they occur.
 * @property web3 The [Web3j] instance that [BlockchainService] uses to interact with the
 * EVM-compatible blockchain.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property creds Blockchain credentials used for signing transactions.
 * @property lastParsedBlockNum The block number of the most recently parsed block.
 * @property newestBlockNum The block number of the most recently confirmed block.
 * @property listenInterval The number of milliseconds that [BlockchainService] should wait after
 * parsing a block before it begins parsing another block.
 * @property listenJob The coroutine [Job] in which [BlockchainService] listens to the blockchain.
 * @property runLoop Boolean that indicates whether [listenLoop] should continue to execute its
 * loop.
 * @property gasProvider Provides gas price information to [commutoSwap].
 * @property txManager A [CommutoTransactionManager] for [commutoSwap]
 * @property commutoSwap A [CommutoSwap] instance that [BlockchainService] uses to parse transaction
 * receipts for CommutoSwap events and interact with the [CommutoSwap contract](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol)
 * on chain.
 */
@Singleton
class BlockchainService (private val exceptionHandler: BlockchainExceptionNotifiable,
                         private val offerService: OfferNotifiable,
                         private val web3: Web3j,
                         commutoSwapAddress: String) {

    @Inject constructor(errorHandler: BlockchainExceptionNotifiable, offerService: OfferNotifiable) :
            this(errorHandler,
                offerService,
                Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE"))),
                "0x687F36336FCAB8747be1D41366A416b41E7E1a96"
            )

    init {
        (offerService as? OfferService)?.setBlockchainService(this)
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val creds: Credentials = Credentials.create(
        "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a"
    )

    private var lastParsedBlockNum: BigInteger = BigInteger.ZERO

    private var newestBlockNum: BigInteger = BigInteger.ZERO

    // TODO: rename this as updateLastParsedBlockNumber
    /**
     * Updates [lastParsedBlockNum]. Eventually, this function will store [blockNumber] in
     * persistent storage.
     *
     * @param blockNumber The block number of the block that has been most recently parsed by
     * [BlockchainService], to be set as [lastParsedBlockNum].
     */
    private fun setLastParsedBlockNumber(blockNumber: BigInteger) {
        lastParsedBlockNum = blockNumber
    }

    private val listenInterval = 30L

    private var listenJob: Job = Job()

    private var runLoop = true

    private val gasProvider = DumbGasProvider()

    private val txManager = CommutoTransactionManager(web3, creds, ChainIdLong.NONE)

    private val commutoSwap: CommutoSwap = CommutoSwap.load(
        commutoSwapAddress,
        web3,
        txManager,
        gasProvider
    )

    /**
     * Launches a new coroutine [Job] in [GlobalScope], the global coroutine scope, runs
     * [listenLoop] in this new [Job], and stores a reference to it in [listenJob].
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        logger.info("Starting listen loop in global coroutine scope")
        listenJob = GlobalScope.launch {
            runLoop = true
            listenLoop()
        }
        logger.info("Starting listen loop in global coroutine scope")
    }

    /**
     * Sets [runLoop] to false to prevent the listen loop from being executed again and cancels
     * [listenJob].
     */
    fun stopListening() {
        logger.info("Stopping listen loop and canceling listen job")
        runLoop = false
        listenJob.cancel()
        logger.info("Stopped listen loop and canceled listen job")
    }

    /**
     * Executes the listening process in the current coroutine context as long as [runLoop] is true.
     *
     * Listening Process:
     *
     * First, we get the block number of the most recently confirmed block, and update
     * [newestBlockNum] with this value. Then we compare this to the number of the most recently
     * parsed block. If the newest block number is greater than that of the most recently parsed
     * block, then there exists at least one new block that we must parse. If the newest block
     * number is not greater than the last parsed block number, then we don't have a new block to
     * parse, and we delay the coroutine in which we are running by [listenInterval] milliseconds.
     *
     * If we do have at least one new block to parse, we get the block with a block number one
     * greater than that of the last parsed block. Then we parse this new block, and then set the
     * last parsed block number as the block number of this newly parsed block.
     *
     * If we encounter an [Exception], we pass it to [exceptionHandler]. Additionally, if the
     * exception is a [ConnectException], indicating that we are having problems communicating with
     * the network node, then we stop listening.
     */
    suspend fun listenLoop() {
        while (runLoop) {
            try {
                logger.info("Beginning iteration of listen loop, last parsed block number: $lastParsedBlockNum")
                newestBlockNum = getNewestBlockNumberAsync().await().blockNumber
                if (newestBlockNum > lastParsedBlockNum) {
                    logger.info("Newest block number $newestBlockNum > last parsed block number $lastParsedBlockNum")
                    val block = getBlockAsync(lastParsedBlockNum + BigInteger.ONE).await()
                        .block
                    logger.info("Got block ${block.number}")
                    parseBlock(block)
                    logger.info("Parsed block ${block.number}")
                    setLastParsedBlockNumber(block.number)
                    logger.info("Updated last parsed block number as ${block.number}")
                } else {
                    logger.info("Newest block number $newestBlockNum <= last parsed block number " +
                            "$lastParsedBlockNum, delaying for $listenInterval ms")
                    delay(listenInterval)
                }
            } catch (e: Exception) {
                logger.error("Got an exception during listen loop, calling exception handler", e)
                exceptionHandler.handleBlockchainException(e)
                if (e is ConnectException) {
                    logger.error("Caught ConnectionException, stopping listening loop", e)
                    stopListening()
                }
            }
            logger.info("Completed iteration of listen loop")
        }
    }

    /**
     * A [Deferred] wrapper around Web3j's [Web3j.ethBlockNumber] method.
     *
     * @return A [Deferred] with an [EthBlockNumber] result.
     */
    private fun getNewestBlockNumberAsync(): Deferred<EthBlockNumber> {
        return web3.ethBlockNumber().sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around Web3j's [Web3j.ethGetBlockByNumber] method.
     *
     * Note: Blocks returned by this function do not contain complete transactions, they contain
     * transaction hashes instead.
     *
     * @param blockNumber The block number of the block to be returned.
     *
     * @return A [Deferred] with an [EthBlock] result.
     */
    private fun getBlockAsync(blockNumber: BigInteger): Deferred<EthBlock> {
        return web3.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(blockNumber),
            false
        ).sendAsync().asDeferred()
    }

    /**
     * Gets full transaction receipts for transactions with the specified hashes.
     *
     * @param txHashes A list of transaction hashes (as [String]s) for which to get full
     * transaction receipts (as [EthGetTransactionReceipt]s).
     *
     * @return A [List] of [Deferred]s with [EthGetTransactionReceipt] results.
     */
    private fun getDeferredTxReceiptOptionals (
        txHashes: List<String>
    ): List<Deferred<EthGetTransactionReceipt>> {
        return txHashes.map {
            web3.ethGetTransactionReceipt(it).sendAsync().asDeferred()
        }
    }

    /**
     * Uses the [CommutoSwap.getOffer] method and the [Web3j.ethChainId] to get the current chain ID and all on-chain
     * data about the offer with the specified ID, and creates and returns an [OfferStruct] with the results.
     *
     * @param id The ID of the offer to return.
     *
     * @return An [OfferStruct] containing all on-chain data of the offer with the specified ID, or null if no such
     * offer exists.
     */
    suspend fun getOffer(id: UUID): OfferStruct? {
        val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIdByteBuffer.putLong(id.mostSignificantBits)
        offerIdByteBuffer.putLong(id.leastSignificantBits)
        val offer = commutoSwap.getOffer(offerIdByteBuffer.array()).sendAsync().asDeferred()
        val chainID = web3.ethChainId().sendAsync().asDeferred()
        return OfferStruct.createFromGetOfferResponse(offer.await(), chainID.await().chainId)
    }

    /**
     * A [Deferred] wrapper around [CommutoSwap.serviceFeeRate] method.
     *
     * @return A [Deferred] with a [BigInteger] result.
     */
    fun getServiceFeeRateAsync(): Deferred<BigInteger> {
        return commutoSwap.serviceFeeRate.sendAsync().asDeferred()
    }

    /**
     * Parses the given [EthBlock.Block] in search of
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol)
     * events, creates a list of all such events that it finds, and then calls
     * [handleEventResponses], passing said list of events. (Specifically, the events are
     * [BaseEventResponse]s)
     *
     * @param block The [EthBlock.Block] to be parsed.
     */
    private suspend fun parseBlock(block: EthBlock.Block) {
        val chainID = web3.ethChainId().sendAsync().await().chainId
        val txHashes: List<String> = block.transactions.mapNotNull {
            when (it) {
                is EthBlock.TransactionHash -> {
                    it.get()
                }
                is EthBlock.TransactionObject -> {
                    it.get().hash
                }
                else -> {
                    null
                }
            }
        }
        val deferredTxReceiptOptionals = getDeferredTxReceiptOptionals(txHashes)
        val eventResponses: MutableList<List<BaseEventResponse>> = mutableListOf()
        for (deferredReceiptOptional in deferredTxReceiptOptionals) {
            eventResponses.add(parseDeferredReceiptOptional(deferredReceiptOptional))
        }
        handleEventResponses(eventResponses, chainID)
    }

    /**
     * Awaits the given [Deferred] and gets event responses from the resulting
     * [EthGetTransactionReceipt].
     *
     * @param deferredReceiptOptional A [Deferred] with a [EthGetTransactionReceipt] result.
     *
     * @return A [List] of [BaseEventResponse]s present in the [EthGetTransactionReceipt] of
     * [deferredReceiptOptional].
     */
    private suspend fun parseDeferredReceiptOptional(
        deferredReceiptOptional: Deferred<EthGetTransactionReceipt>
    ): List<BaseEventResponse> = coroutineScope {
        val receiptOptional = deferredReceiptOptional.await()
        if (receiptOptional.transactionReceipt.isPresent) {
            getEventResponsesFromReceipt(receiptOptional.transactionReceipt.get())
        } else {
            emptyList()
        }
    }

    /**
     * Parses a given [TransactionReceipt] in search of
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol)
     * events, and returns any such events that are found.
     *
     * @param receipt The [TransactionReceipt] to parse.
     *
     * @return A [List] of [BaseEventResponse]s, which are CommutoSwap events.
     */
    private fun getEventResponsesFromReceipt(receipt: TransactionReceipt): List<BaseEventResponse> {
        val eventResponses: MutableList<List<BaseEventResponse>> = mutableListOf()
        if (receipt.to.equals(commutoSwap.contractAddress, ignoreCase = true)) {
            eventResponses.add(commutoSwap.getOfferOpenedEvents(receipt))
            eventResponses.add(commutoSwap.getOfferEditedEvents(receipt))
            eventResponses.add(commutoSwap.getOfferCanceledEvents(receipt))
            eventResponses.add(commutoSwap.getOfferTakenEvents(receipt))
            eventResponses.add(commutoSwap.getServiceFeeRateChangedEvents(receipt))
        }
        return eventResponses.flatten()
    }

    /**
     * Flattens and then iterates through [eventResponseLists] in search of relevant
     * [BaseEventResponse]s, and creates event objects and passes them to the proper service.
     *
     * @param eventResponseLists A [MutableList] of [List]s of [BaseEventResponse]s, which are
     * relevant events about which other services must be notified.
     */
    private suspend fun handleEventResponses(
        eventResponseLists: MutableList<List<BaseEventResponse>>,
        chainID: BigInteger
    ) {
        val eventResponses = eventResponseLists.flatten()
        logger.info("handleEventResponses: handling ${eventResponses.size} events")
        for (eventResponse in eventResponses) {
            when (eventResponse) {
                is CommutoSwap.OfferOpenedEventResponse -> {
                    logger.info("handleEventResponses: handling OfferOpenedEvent")
                    offerService.handleOfferOpenedEvent(OfferOpenedEvent.fromEventResponse(eventResponse, chainID))
                }
                is CommutoSwap.OfferEditedEventResponse -> {
                    logger.info("handleEventResponses: handling OfferEditedEvent")
                    offerService.handleOfferEditedEvent(OfferEditedEvent.fromEventResponse(eventResponse, chainID))
                }
                is CommutoSwap.OfferCanceledEventResponse -> {
                    logger.info("handleEventResponses: handling OfferCanceledEvent")
                    offerService.handleOfferCanceledEvent(OfferCanceledEvent.fromEventResponse(eventResponse, chainID))
                }
                is CommutoSwap.OfferTakenEventResponse -> {
                    logger.info("handleEventResponses: handling OfferTakenEvent")
                    offerService.handleOfferTakenEvent(OfferTakenEvent.fromEventResponse(eventResponse, chainID))
                }
                is CommutoSwap.ServiceFeeRateChangedEventResponse -> {
                    logger.info("handleEventResponses: handling ServiceFeeRateChangedEvent")
                    offerService.handleServiceFeeRateChangedEvent(
                        ServiceFeeRateChangedEvent.fromEventResponse(eventResponse)
                    )
                }
            }
        }
    }

}