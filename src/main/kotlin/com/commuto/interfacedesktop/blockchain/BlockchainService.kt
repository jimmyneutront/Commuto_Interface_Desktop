package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.CommutoWeb3j
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.events.erc20.ApprovalEvent
import com.commuto.interfacedesktop.blockchain.events.erc20.CommutoApprovalEventResponse
import com.commuto.interfacedesktop.blockchain.events.erc20.TokenTransferApprovalPurpose
import com.commuto.interfacedesktop.blockchain.structs.OfferStruct
import com.commuto.interfacedesktop.blockchain.structs.SwapStruct
import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import com.commuto.interfacedesktop.contractwrapper.CommutoSwap.Dispute
import com.commuto.interfacedesktop.dispute.DisputeNotifiable
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.oldcontractwrapper.CommutoFunctionEncoder
import com.commuto.interfacedesktop.swap.SwapNotifiable
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.*
import org.web3j.protocol.http.HttpService
import org.web3j.service.TxSignServiceImpl
import org.web3j.tx.ChainIdLong
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.net.ConnectException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap
import kotlin.math.floor

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
 * @property swapService An object to which [BlockchainService] will pass swap-related events when they occur.
 * @property disputeService An object to which [BlockchainService] will pass dispute-related events when they occur.
 * @property web3 The [Web3j] instance that [BlockchainService] uses to interact with the
 * EVM-compatible blockchain.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property creds Blockchain credentials used for signing transactions.
 * @property lastParsedBlockNum The block number of the most recently parsed block.
 * @property newestBlockNum The block number of the most recently confirmed block.
 * @property transactionsToMonitor A dictionary mapping transaction hashes to corresponding [BlockchainTransaction]s
 * created by this interface that [BlockchainService] will monitor for confirmation, transaction dropping, transaction
 * failure and transaction success.
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
                         private val swapService: SwapNotifiable,
                         private val disputeService: DisputeNotifiable,
                         private val web3: CommutoWeb3j,
                         commutoSwapAddress: String) {

    @Inject constructor(
        errorHandler: BlockchainExceptionNotifiable,
        offerService: OfferNotifiable,
        swapService: SwapNotifiable,
        disputeService: DisputeNotifiable,
    ):
            this(errorHandler,
                offerService,
                swapService,
                disputeService,
                CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE"))),
                "0x687F36336FCAB8747be1D41366A416b41E7E1a96"
            )

    init {
        (offerService as? OfferService)?.setBlockchainService(this)
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val creds: Credentials = Credentials.create(
        "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d"
    )

    /**
     * Returns the address of [creds] as a string.
     */
    fun getAddress(): String {
        return creds.address
    }

    private var lastParsedBlockNum: BigInteger = BigInteger.ZERO

    var newestBlockNum: BigInteger = BigInteger.ZERO
        get() = field
        private set(value) {
            field = value
        }

    private var transactionsToMonitor = HashMap<String, BlockchainTransaction>()

    fun addTransactionToMonitor(transaction: BlockchainTransaction) {
        transactionsToMonitor[transaction.transactionHash] = transaction
    }

    fun getMonitoredTransaction(transactionHash: String): BlockchainTransaction? {
        return transactionsToMonitor[transactionHash]
    }

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

    private val listenInterval = 3_000L

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
     * Returns the contract address of [commutoSwap].
     */
    fun getCommutoSwapAddress(): String {
        return commutoSwap.contractAddress
    }

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
     * Sets [runLoop] to false to prevent the listen loop from being executed again.
     */
    fun stopListening() {
        logger.info("Stopping listen loop")
        runLoop = false
        logger.info("Stopped listen loop")
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
     * Uses the [CommutoSwap.getOffer] method and [Web3j.ethChainId] to get the current chain ID and all on-chain data
     * about the offer with the specified ID, and creates and returns an [OfferStruct] with the results.
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
     * Stores [transaction] in [transactionsToMonitor] and then sends the wrapped [RawTransaction] to the blockchain
     * node via a call to [eth_sendRawTransaction](https://ethereum.github.io/execution-apis/api-documentation/). This
     * signs [transaction] with [creds] and [chainID] and then converts the result to a hex string. If this hex string
     * is not equal to [signedRawTransactionDataAsHex], this throws an [IllegalStateException].
     *
     * @param transaction The [BlockchainTransaction] containing the [RawTransaction] from which
     * [signedRawTransactionDataAsHex] was created, to be sent to the node as a raw transaction.
     * @param signedRawTransactionDataAsHex The signed raw transaction wrapped by [BlockchainTransaction] as a
     * hexadecimal string, which will be sent to the blockchain node.
     * @param chainID The ID of the blockchain to which this transaction should be sent.
     *
     * @return A [Deferred] with a [EthSendTransaction] result.
     *
     * @throws [IllegalStateException] if the transaction wrapped by [transaction] and [chainID] do not correspond to
     * [signedRawTransactionDataAsHex].
     */
    suspend fun sendTransaction(
        transaction: BlockchainTransaction,
        signedRawTransactionDataAsHex: String,
        chainID: BigInteger
    ): EthSendTransaction {
        val wrappedTransaction = transaction.transaction
            ?: throw BlockchainServiceException(message = "Wrapped transaction was nil for " +
                    transaction.transactionHash
            )
        check(signedRawTransactionDataAsHex ==
                Numeric.toHexString(TxSignServiceImpl(creds).sign(wrappedTransaction, chainID.toLong()))
        ) {
            "Supplied signed transaction data and actual signed transaction data do not match"
        }
        transactionsToMonitor[transaction.transactionHash] = transaction
        try {
            return web3.ethSendRawTransaction(signedRawTransactionDataAsHex).sendAsync().await()
        } catch (exception: Exception) {
            transactionsToMonitor.remove(transaction.transactionHash)
            throw exception
        }
    }

    /**
     * Signs the given transaction with [creds] for the blockchain specified by [chainID], and returns the resulting
     * [ByteArray].
     *
     * @param transaction The [RawTransaction] to be signed.
     * @param chainID The ID of the blockchain for which [transaction] should be signed.
     *
     * @return The [ByteArray] that is the signed transaction.
     */
    fun signTransaction(transaction: RawTransaction, chainID: BigInteger): ByteArray {
        return TxSignServiceImpl(creds).sign(transaction, chainID.toLong())
    }

    /**
     * A [Deferred] wrapper around the [ERC20](https://eips.ethereum.org/EIPS/eip-20) `approve` function.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun approveTokenTransferAsync(
        tokenAddress: String,
        destinationAddress: String,
        amount: BigInteger
    ): Deferred<TransactionReceipt> {
        val tokenContract = ERC20.load(
            tokenAddress,
            web3,
            txManager,
            gasProvider
        )
        return tokenContract.approve(destinationAddress, amount).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.openOffer] method.
     *
     * @param id The ID of the new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * opened.
     * @param offerStruct The [OfferStruct] containing the data of the new
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be opened.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun openOfferAsync(id: UUID, offerStruct: OfferStruct): Deferred<TransactionReceipt> {
        val iDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        iDByteBuffer.putLong(id.mostSignificantBits)
        iDByteBuffer.putLong(id.leastSignificantBits)
        val iDByteArray = iDByteBuffer.array()
        return commutoSwap.openOffer(iDByteArray, offerStruct.toCommutoSwapOffer()).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.cancelOffer] method.
     *
     * @param id The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * canceled.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun cancelOfferAsync(id: UUID): Deferred<TransactionReceipt> {
        val iDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        iDByteBuffer.putLong(id.mostSignificantBits)
        iDByteBuffer.putLong(id.leastSignificantBits)
        val iDByteArray = iDByteBuffer.array()
        return commutoSwap.cancelOffer(iDByteArray).sendAsync().asDeferred()
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the user's account to call the
     * [approve](https://ethereum.org/en/developers/docs/standards/tokens/erc-20/) function of an ERC20 contract, with
     * estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently
     * known transactions, including those that are still pending.
     *
     * @param tokenAddress The address of the ERC20 contract to call.
     * @param spender The address that will be given permission to spend some of the user's balance of [tokenAddress]
     * tokens.
     * @param amount The amount of the user's [tokenAddress] tokens that [spender] will be allowed to spend.
     *
     * @Return A [RawTransaction] as described above, that will allow [spender] to spend [amount] of the user's
     * [tokenAddress] tokens.
     */
    suspend fun createApproveTransferTransaction(
        tokenAddress: String,
        spender: String,
        amount: BigInteger
    ): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "approve",
            listOf(org.web3j.abi.datatypes.Address(spender), org.web3j.abi.datatypes.generated.Uint256(amount)),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val chainID = web3.ethChainId().sendAsync().asDeferred().await().chainId
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            tokenAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the user's account to call CommutoSwap's
     * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) function, with estimated
     * gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently known
     * transactions, including those that are still pending.
     *
     * @param offerID The ID of the new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to
     * be opened.
     * @param offerStruct The OfferStruct containing the data of the new
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be opened.
     *
     * @return A [RawTransaction] as described above, that will open an offer using the data supplied in [offerStruct]
     * with the ID specified by [offerID].
     */
    suspend fun createOpenOfferTransaction(offerID: UUID, offerStruct: OfferStruct): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "openOffer",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(offerID.asByteArray()), offerStruct.toCommutoSwapOffer()),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            offerStruct.chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            offerStruct.chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [cancelOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#cancel-offer) function, with
     * estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently
     * known transactions, including those that are still pending.
     *
     * @param offerID The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * canceled.
     * @param chainID The blockchain ID on which the offer to be canceled exists.
     *
     * @return A [RawTransaction] as described above, that will cancel the offer specified by [offerID] on the chain
     * specified by [chainID].
     */
    suspend fun createCancelOfferTransaction(offerID: UUID, chainID: BigInteger): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "cancelOffer",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(offerID.asByteArray())),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [editOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#edit-offer) function, with
     * estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently
     * known transactions, including those that are still pending.
     *
     * @param offerID The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * edited.
     * @param chainID The blockchain ID on which the offer to be edited exists.
     * @param offerStruct An [OfferStruct] containing the new data with which the offer will be updated.
     *
     * @return A [RawTransaction] as described above, that will edit the offer specified by [offerID] on the chain
     * specified by [chainID] using certain data contained in [offerStruct].
     */
    suspend fun createEditOfferTransaction(
        offerID: UUID,
        chainID: BigInteger,
        offerStruct: OfferStruct
    ): RawTransaction {
        val editedOffer = CommutoSwap.Offer(
            offerStruct.isCreated,
            offerStruct.isTaken,
            offerStruct.maker,
            offerStruct.interfaceID,
            offerStruct.stablecoin,
            offerStruct.amountLowerBound,
            offerStruct.amountUpperBound,
            offerStruct.securityDepositAmount,
            offerStruct.serviceFeeRate,
            offerStruct.direction,
            offerStruct.settlementMethods,
            offerStruct.protocolVersion
        )
        val function = org.web3j.abi.datatypes.Function(
            "editOffer",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(offerID.asByteArray()), editedOffer),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the user's account to call CommutoSwap's
     * [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer) function, with estimated
     * gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently known
     * transactions, including those that are still pending.
     *
     * @param offerID The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * taken.
     * @param swapStruct The [SwapStruct] containing the data of the
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be taken.
     *
     * @return A [RawTransaction] as described above, that will take the offer specified by [offerID] using the data
     * supplied in [swapStruct].
     */
    suspend fun createTakeOfferTransaction(offerID: UUID, swapStruct: SwapStruct): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "takeOffer",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(offerID.asByteArray()), swapStruct.toCommutoSwapSwap()),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            swapStruct.chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            swapStruct.chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) function, with estimated gas
     * limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently known
     * transactions, including those that are still pending.
     *
     * @param swapID The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) to be
     * filled.
     * @param chainID The blockchain ID on which the swap exists.
     *
     * @return A [RawTransaction] as described above, that will call
     * [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) for the swap specified by
     * [swapID] on the chain specified by [chainID].
     */
    suspend fun createFillSwapTransaction(swapID: UUID, chainID: BigInteger): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "fillSwap",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(swapID.asByteArray())),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) function,
     * with estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all
     * currently known transactions, including those that are still pending.
     *
     * @param swapID The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which
     * payment sending will be reported.
     * @param chainID The blockchain ID on which the swap exists.
     *
     * @return A [RawTransaction] as described above, that will call
     * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent) for the
     * swap specified by [swapID] on the chain specified by [chainID].
     */
    suspend fun createReportPaymentSentTransaction(swapID: UUID, chainID: BigInteger): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "reportPaymentSent",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(swapID.asByteArray())),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * function, with estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from
     * all currently known transactions, including those that are still pending.
     *
     * @param swapID The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which
     * payment receiving will be reported.
     * @param chainID The blockchain ID on which the swap exists.
     *
     * @return A [RawTransaction] as described above, that will call
     * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
     * for the swap specified by [swapID] on the chain specified by [chainID].
     */
    suspend fun createReportPaymentReceivedTransaction(swapID: UUID, chainID: BigInteger): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "reportPaymentReceived",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(swapID.asByteArray())),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the users account to call CommutoSwap's
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap)
     * function, with estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from
     * all currently known transactions, including those that are still pending.
     *
     * @param swapID The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) that will
     * be closed.
     * @param chainID The blockchain ID on which the swap exists.
     *
     * @return A [RawTransaction] as described above, that will call
     * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap) for the swap specified by
     * [swapID] on the chain specified by [chainID].
     */
    suspend fun createCloseSwapTransaction(swapID: UUID, chainID: BigInteger): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "closeSwap",
            listOf(org.web3j.abi.datatypes.generated.Bytes16(swapID.asByteArray())),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.editOffer] method.
     *
     * @param offerID The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be
     * edited.
     * @param offerStruct The [OfferStruct] from which the [CommutoSwap.Offer] to be passed to
     * [editOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#edit-offer) will be derived.
     */
    fun editOfferAsync(offerID: UUID, offerStruct: OfferStruct): Deferred<TransactionReceipt> {
        val iDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        iDByteBuffer.putLong(offerID.mostSignificantBits)
        iDByteBuffer.putLong(offerID.leastSignificantBits)
        val iDByteArray = iDByteBuffer.array()

        val offer = CommutoSwap.Offer(
            offerStruct.isCreated,
            offerStruct.isTaken,
            offerStruct.maker,
            offerStruct.interfaceID,
            offerStruct.stablecoin,
            offerStruct.amountLowerBound,
            offerStruct.amountUpperBound,
            offerStruct.securityDepositAmount,
            offerStruct.serviceFeeRate,
            offerStruct.direction,
            offerStruct.settlementMethods,
            offerStruct.protocolVersion,
        )

        return commutoSwap.editOffer(
            iDByteArray,
            offer
        ).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.takeOffer] method.
     *
     * @param id The ID of the [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) to be taken.
     * @param swapStruct The [SwapStruct] containing data necessary to take the offer.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun takeOfferAsync(id: UUID, swapStruct: SwapStruct): Deferred<TransactionReceipt> {
        return commutoSwap.takeOffer(id.asByteArray(), swapStruct.toCommutoSwapSwap()).sendAsync().asDeferred()
    }

    /**
     * Uses the [CommutoSwap.getSwap] method and [Web3j.ethChainId] to get the current chain ID and all on-chain data
     * about the swap with the specified ID, and creates and returns an [SwapStruct] with the results.
     *
     * @param id The ID of the swap to return.
     *
     * @return A [SwapStruct] containing all on-chain data of the swap with the specified ID, or null if no such swap
     * exists.
     */
    suspend fun getSwap(id: UUID): SwapStruct? {
        val swap = commutoSwap.getSwap(id.asByteArray()).sendAsync().asDeferred()
        val chainID = web3.ethChainId().sendAsync().asDeferred()
        return SwapStruct.createFromGetSwapResponse(swap.await(), chainID.await().chainId)
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.fillSwap] method.
     *
     * @param id The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) to be filled.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun fillSwapAsync(id: UUID): Deferred<TransactionReceipt> {
        return commutoSwap.fillSwap(id.asByteArray()).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.reportPaymentSent] method.
     *
     * @param id The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which to
     * report sending payment.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun reportPaymentSentAsync(id: UUID): Deferred<TransactionReceipt> {
        return commutoSwap.reportPaymentSent(id.asByteArray()).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.reportPaymentReceived] method.
     *
     * @param id The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which to
     * report receiving payment.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun reportPaymentReceivedAsync(id: UUID): Deferred<TransactionReceipt> {
        return commutoSwap.reportPaymentReceived(id.asByteArray()).sendAsync().asDeferred()
    }

    /**
     * A [Deferred] wrapper around the [CommutoSwap.closeSwap] method.
     *
     * @param id The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) to close.
     *
     * @return A [Deferred] with a [TransactionReceipt] result.
     */
    fun closeSwapAsync(id: UUID): Deferred<TransactionReceipt> {
        return commutoSwap.closeSwap(id.asByteArray()).sendAsync().asDeferred()
    }

    /**
     * Uses the [CommutoSwap.getActiveDisputeAgents] method to get the addresses of all active dispute agents.
     *
     * @return A [Deferred] that results in a [MutableList] of [String]s, which are the addresses of all active dispute
     * agents.
     */
    fun getActiveDisputeAgentsAsync(): Deferred<MutableList<Any?>> {
        return commutoSwap.activeDisputeAgents.sendAsync().asDeferred()
    }

    /**
     * Creates and returns an EIP1559 [RawTransaction] from the user's account to call CommutoSwap's
     * [raiseDispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#raise-dispute) function, with
     * estimated gas limit, max priority fee per gas, max fee per gas, and with a nonce determined from all currently
     * known transactions, including those that are still pending.
     *
     * @param swapID The ID of the [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) for which
     * a dispute will be raised.
     * @param chainID The ID of the blockchain on which the swap with the ID specified by [swapID] exists.
     * @param disputeAgent0 The address of the first selected active dispute agent.
     * @param disputeAgent1 The address of the second selected active dispute agent.
     * @param disputeAgent2 The address of the third selected active dispute agent.
     *
     * @return A [RawTransaction] as described above, that will raise a dispute for the swap specified by [swapID] on
     * the chain specified by [chainID], using the supplied dispute agent addresses.
     */
    suspend fun createRaiseDisputeTransaction(
        swapID: UUID,
        chainID: BigInteger,
        disputeAgent0: String,
        disputeAgent1: String,
        disputeAgent2: String
    ): RawTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "raiseDispute",
            listOf(
                org.web3j.abi.datatypes.generated.Bytes16(swapID.asByteArray()),
                org.web3j.abi.datatypes.Address(160, disputeAgent0),
                org.web3j.abi.datatypes.Address(160, disputeAgent1),
                org.web3j.abi.datatypes.Address(160, disputeAgent2)
            ),
            listOf()
        )
        val encodedFunction = CommutoFunctionEncoder.encode(function)
        val transactionForGasEstimate = Transaction(
            creds.address.toString(),
            BigInteger.ZERO,
            null, // No gasPrice because we are specifying maxFeePerGas
            BigInteger.valueOf(30_000_000),
            commutoSwap.contractAddress,
            BigInteger.ZERO,
            encodedFunction,
            chainID.toLong(),
            BigInteger.valueOf(1_000_000), // maxPriorityFeePerGas (temporary value)
            BigInteger.valueOf(875_000_000), // maxFeePerGas (temporary value)
        )
        val nonce = web3.ethGetTransactionCount(
            creds.address,
            DefaultBlockParameter.valueOf("pending")
        ).sendAsync().asDeferred()
        val gasLimit = web3.ethEstimateGas(transactionForGasEstimate).sendAsync().asDeferred().await().amountUsed
        // Get the fee history from the last 20 blocks, from the 75th to the 100th percentile.
        val feeHistory = web3.ethFeeHistory(
            20,
            DefaultBlockParameter.valueOf("latest"),
            listOf(75.0)
        ).sendAsync().asDeferred().await().feeHistory
        // Calculate the average of the 75th percentile reward values from the last 20 blocks and use this as the
        // maxPriorityFeePerGas
        val maxPriorityFeePerGas = BigInteger.ZERO.let { finalTipFee ->
            feeHistory.reward.map { it.first() }.forEach { finalTipFee.add(it) }
            finalTipFee.divide(BigInteger.valueOf(feeHistory.reward.count().toLong()))
        }
        // Calculate the 75th percentile base fee per gas value from the last 20 blocks and use this as the
        // baseFeePerGas
        val baseFeePerGas = BigInteger.ZERO.let {
            val percentileIndex = floor(0.75 * feeHistory.baseFeePerGas.count()).toInt()
            feeHistory.baseFeePerGas.sorted()[percentileIndex]
        }
        val maxFeePerGas = baseFeePerGas + maxPriorityFeePerGas
        return RawTransaction.createTransaction(
            chainID.toLong(),
            nonce.await().transactionCount,
            gasLimit,
            transactionForGasEstimate.to,
            BigInteger.ZERO, // value
            transactionForGasEstimate.data,
            maxPriorityFeePerGas,
            maxFeePerGas
        )
    }

    /**
     * Uses the [CommutoSwap.getDispute] method to get all on-chain data about the dispute for the swap with the
     * specified ID, and returns the deferred result.
     *
     * @param id The ID of the swap corresponding to the dispute to return.
     *
     * @return A [Deferred] with a [Dispute] result.
     */
    fun getDisputeAsync(id: UUID): Deferred<Dispute> {
        return commutoSwap.getDispute(id.asByteArray()).sendAsync().asDeferred()
    }

    /**
     * Parses the given [EthBlock.Block] in search of
     * [CommutoSwap](https://github.com/jimmyneutront/commuto-protocol/blob/main/CommutoSwap.sol)
     * events, and creates a list of all such events that it finds. Then this iterates through all remaining monitored
     * transactions. Then this iterates through all remaining monitored transactions. If this finds transactions that
     * have been dropped or have been pending for more than 24 hours, this removes them from [transactionsToMonitor] and
     * calls the appropriate failure handler. Then this calls [handleEventResponses], passing said list of events.
     * (Specifically, the events are [BaseEventResponse]s)
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
        for (monitoredTransaction in transactionsToMonitor.values) {
            logger.info("parseBlock: checking if unconfirmed tx ${monitoredTransaction.transactionHash} is dropped " +
                    "or pending")
            // 86_400_000 milliseconds = 24 hours
            if (Date().time - monitoredTransaction.timeOfCreation.time > 86_400_000) {
                logger.info("parseBlock: monitored tx ${monitoredTransaction.transactionHash} is more than 24 hours " +
                        "old")
                var monitoredTransactionException: BlockchainTransactionException? = null
                val monitoredTransactionReceiptOptional = web3
                    .ethGetTransactionReceipt(monitoredTransaction.transactionHash).sendAsync().asDeferred().await()
                    .transactionReceipt
                var isTransactionNotComfirmed = false
                if (monitoredTransactionReceiptOptional.isPresent) {
                    val monitoredTransactionReceipt = monitoredTransactionReceiptOptional.get()
                    if (monitoredTransactionReceipt.status == null) {
                        isTransactionNotComfirmed = true
                    }
                } else {
                    isTransactionNotComfirmed = true
                }
                if (isTransactionNotComfirmed) {
                    monitoredTransactionException = BlockchainTransactionException("Transaction " +
                            "${monitoredTransaction.transactionHash} has been pending for more than 24 hours.")
                }
                if (monitoredTransactionException != null) {
                    logger.info("parseBlock: removing from transactionsToMonitor and handling failed monitored tx " +
                            "${monitoredTransaction.transactionHash} of type ${monitoredTransaction.type.asString} " +
                            "for reason: ${monitoredTransactionException.message}")
                    transactionsToMonitor.remove(monitoredTransaction.transactionHash)
                    when (monitoredTransaction.type) {
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER,
                        BlockchainTransactionType.OPEN_OFFER,
                        BlockchainTransactionType.CANCEL_OFFER, BlockchainTransactionType.EDIT_OFFER,
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER,
                        BlockchainTransactionType.TAKE_OFFER -> {
                            offerService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = monitoredTransactionException
                            )
                        }
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP,
                        BlockchainTransactionType.FILL_SWAP,
                        BlockchainTransactionType.REPORT_PAYMENT_SENT,
                        BlockchainTransactionType.REPORT_PAYMENT_RECEIVED,
                        BlockchainTransactionType.CLOSE_SWAP -> {
                            swapService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = monitoredTransactionException
                            )
                        }
                        BlockchainTransactionType.RAISE_DISPUTE -> {
                            disputeService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = monitoredTransactionException
                            )
                        }
                    }
                }
            }
        }
        handleEventResponses(eventResponses, chainID)
    }

    /**
     * Awaits the given [Deferred] and attempts to get a [TransactionReceipt] from the resulting
     * [EthGetTransactionReceipt]. If the [EthGetTransactionReceipt] does not contain a [TransactionReceipt], this
     * returns an empty list, since a nonexistent transaction receipt contains no events. If this does get a
     * [TransactionReceipt] from [EthGetTransactionReceipt], this searches for a monitored transaction with a matching
     * transaction hash. If it finds such a transaction, it checks if the status of the corresponding
     * [TransactionReceipt] is OK. If it is, then this parses it for the proper type of event and adds resulting events
     * to a list of events that will be returned. If it is not OK, then this calls the appropriate failure handler. In
     * either case, we then remove the transaction from [transactionsToMonitor]. If the hash specified in
     * [TransactionReceipt] is not present in [transactionsToMonitor], then this parses it for events and appends any
     * resulting events to the list of events that will be returned. Finally, this returns said list of events.
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
            val transactionReceipt = receiptOptional.transactionReceipt.get()
            val eventsInReceipt = mutableListOf<BaseEventResponse>()
            val monitoredTransaction = transactionsToMonitor[transactionReceipt.transactionHash]
            if (monitoredTransaction != null) {
                logger.info("parseDeferredReceiptOptional: ${transactionReceipt.transactionHash} is monitored, working")
                if (transactionReceipt.isStatusOK) {
                    logger.info("parseDeferredReceiptOptional: parsing monitored tx " +
                            "${transactionReceipt.transactionHash} of type ${monitoredTransaction.type.asString} for " +
                            "events")
                    // The tranaction has not failed, so we parse it for the proper event
                    when (monitoredTransaction.type) {
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER,
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER,
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP -> {
                            eventsInReceipt.addAll(parseApprovalTransaction(
                                transactionReceipt = transactionReceipt,
                                monitoredTransaction = monitoredTransaction
                            ))
                        }
                        BlockchainTransactionType.OPEN_OFFER -> {
                            eventsInReceipt.addAll(commutoSwap.getOfferOpenedEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.CANCEL_OFFER -> {
                            eventsInReceipt.addAll(commutoSwap.getOfferCanceledEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.EDIT_OFFER -> {
                            eventsInReceipt.addAll(commutoSwap.getOfferEditedEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.TAKE_OFFER -> {
                            eventsInReceipt.addAll(commutoSwap.getOfferTakenEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.FILL_SWAP -> {
                            eventsInReceipt.addAll(commutoSwap.getSwapFilledEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.REPORT_PAYMENT_SENT -> {
                            eventsInReceipt.addAll(commutoSwap.getPaymentSentEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.REPORT_PAYMENT_RECEIVED -> {
                            eventsInReceipt.addAll(commutoSwap.getPaymentReceivedEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.CLOSE_SWAP -> {
                            eventsInReceipt.addAll(commutoSwap.getBuyerClosedEvents(transactionReceipt))
                            eventsInReceipt.addAll(commutoSwap.getSellerClosedEvents(transactionReceipt))
                        }
                        BlockchainTransactionType.RAISE_DISPUTE -> {
                            eventsInReceipt.addAll(commutoSwap.getDisputeRaisedEvents(transactionReceipt))
                        }
                    }
                } else {
                    logger.warn("parseDeferredReceiptOptional: monitored tx ${transactionReceipt.transactionHash} of " +
                            "type ${monitoredTransaction.type.asString} failed, calling failure handler")
                    val exception = BlockchainTransactionException(
                        message = "Transaction ${transactionReceipt.transactionHash} is confirmed, but failed for " +
                                "unknown reason."
                    )
                    when (monitoredTransaction.type) {
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER,
                        BlockchainTransactionType.OPEN_OFFER,
                        BlockchainTransactionType.CANCEL_OFFER, BlockchainTransactionType.EDIT_OFFER,
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER,
                        BlockchainTransactionType.TAKE_OFFER, -> {
                            offerService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = exception
                            )
                        }
                        BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP,
                        BlockchainTransactionType.FILL_SWAP,
                        BlockchainTransactionType.REPORT_PAYMENT_SENT,
                        BlockchainTransactionType.REPORT_PAYMENT_RECEIVED,
                        BlockchainTransactionType.CLOSE_SWAP -> {
                            swapService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = exception
                            )
                        }
                        BlockchainTransactionType.RAISE_DISPUTE -> {
                            disputeService.handleFailedTransaction(
                                transaction = monitoredTransaction,
                                exception = exception
                            )
                        }
                    }
                }
                logger.info("parseDeferredReceiptOptional: removing ${transactionReceipt.transactionHash} from " +
                        "transactionsToMonitor")
                // Remove monitored transaction now that it has been handled
                transactionsToMonitor.remove(transactionReceipt.transactionHash)
            } else {
                logger.info("parseDeferredReceiptOptional: tx ${transactionReceipt.transactionHash} is not " +
                        "monitored, parsing for events")
                eventsInReceipt.addAll(getEventResponsesFromReceipt(transactionReceipt))
            }
            eventsInReceipt
        } else {
            emptyList()
        }
    }

    /**
     * Parses a given [TransactionReceipt] corresponding to a [BlockchainTransaction] in search of ERC20
     * [Approve](https://eips.ethereum.org/EIPS/eip-20) events, and returns a corresponding list of
     * [CommutoApprovalEventResponse]s.
     *
     * @param transactionReceipt The [TransactionReceipt] to be parsed.
     * @param monitoredTransaction The [BlockchainTransaction] corresponding to [transactionReceipt].
     *
     * @return A [List] of [CommutoApprovalEventResponse]s created from all
     * [Approve](https://eips.ethereum.org/EIPS/eip-20) events emitted by the transaction.
     */
    private fun parseApprovalTransaction(
        transactionReceipt: TransactionReceipt,
        monitoredTransaction: BlockchainTransaction
    ): List<CommutoApprovalEventResponse> {
        val erc20Contract = ERC20.load(
            "0x0000000000000000000000000000000000000000",
            web3,
            txManager,
            gasProvider
        )
        return erc20Contract.getApprovalEvents(transactionReceipt).map {
            CommutoApprovalEventResponse(
                log = it.log,
                owner = it._owner,
                spender = it._spender,
                amount = it._value,
                eventName = if (monitoredTransaction.type == BlockchainTransactionType
                        .APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER) {
                    "Approval_forOpeningOffer"
                } else if (monitoredTransaction.type == BlockchainTransactionType
                        .APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER) {
                    "Approval_forTakingOffer"
                } else if (monitoredTransaction.type == BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP) {
                    "Approval_forFillingSwap"
                } else {
                    "unknown"
                }
            )
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
            eventResponses.add(commutoSwap.getSwapFilledEvents(receipt))
            eventResponses.add(commutoSwap.getPaymentSentEvents(receipt))
            eventResponses.add(commutoSwap.getPaymentReceivedEvents(receipt))
            eventResponses.add(commutoSwap.getBuyerClosedEvents(receipt))
            eventResponses.add(commutoSwap.getSellerClosedEvents(receipt))
            eventResponses.add(commutoSwap.getDisputeRaisedEvents(receipt))
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
                is CommutoApprovalEventResponse -> {
                    logger.info("handleEventResponse: handling CommutoApprovalEventResponse with eventName " +
                            eventResponse.eventName
                    )
                    if (eventResponse.eventName == "Approval_forOpeningOffer") {
                        offerService.handleTokenTransferApprovalEvent(
                            ApprovalEvent.fromEventResponse(
                                eventResponse,
                                TokenTransferApprovalPurpose.OPEN_OFFER,
                                chainID
                            )
                        )
                    } else if (eventResponse.eventName == "Approval_forTakingOffer") {
                        offerService.handleTokenTransferApprovalEvent(
                            ApprovalEvent.fromEventResponse(
                                eventResponse,
                                TokenTransferApprovalPurpose.TAKE_OFFER,
                                chainID
                            )
                        )
                    } else if (eventResponse.eventName == "Approval_forFillingSwap") {
                        swapService.handleTokenTransferApprovalEvent(
                            ApprovalEvent.fromEventResponse(
                                eventResponse,
                                TokenTransferApprovalPurpose.FILL_SWAP,
                                chainID
                            )
                        )
                    } else {
                        logger.warn("handleEventResponses: got CommutoApprovalEventResponse with unrecognized " +
                                "eventName ${eventResponse.eventName}")
                    }
                }
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
                is CommutoSwap.SwapFilledEventResponse -> {
                    logger.info("handleEventResponses: handling SwapFilledEventResponse")
                    swapService.handleSwapFilledEvent(
                        SwapFilledEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
                is CommutoSwap.PaymentSentEventResponse -> {
                    logger.info("handleEventResponses: handling PaymentSentEventResponse")
                    swapService.handlePaymentSentEvent(
                        PaymentSentEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
                is CommutoSwap.PaymentReceivedEventResponse -> {
                    logger.info("handleEventResponses: handling PaymentReceivedEventResponse")
                    swapService.handlePaymentReceivedEvent(
                        PaymentReceivedEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
                is CommutoSwap.BuyerClosedEventResponse -> {
                    logger.info("handleEventResponses: handling BuyerClosedEventResponse")
                    swapService.handleBuyerClosedEvent(
                        BuyerClosedEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
                is CommutoSwap.SellerClosedEventResponse -> {
                    logger.info("handleEventResponses: handling SellerClosedEventResponse")
                    swapService.handleSellerClosedEvent(
                        SellerClosedEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
                is CommutoSwap.DisputeRaisedEventResponse -> {
                    logger.info("handleEventResponse: handling DisputeRaisedEventResponse")
                    disputeService.handleDisputeRaisedEvent(
                        DisputeRaisedEvent.fromEventResponse(eventResponse, chainID)
                    )
                }
            }
        }
    }

}