package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.CommutoSwap
import com.commuto.interfacedesktop.offer.OfferNotifiable
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.*
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainIdLong
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockchainService (val offerService: OfferNotifiable, commutoSwapAddress: String) {

    @Inject constructor(offerService: OfferNotifiable) :
            this(offerService, "0x687F36336FCAB8747be1D41366A416b41E7E1a96")

    // Blockchain credentials
    private val creds: Credentials = Credentials.create(
        "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a"
    )

    // The number of the last parsed block
    private var lastParsedBlockNum: BigInteger = BigInteger.ZERO

    // The number of the newest block
    private var newestBlockNum: BigInteger = BigInteger.ZERO

    private fun setLastParsedBlockNumber(blockNumber: BigInteger) {
        lastParsedBlockNum = blockNumber
    }

    // The number of milliseconds that BlockchainService should wait after parsing a block
    //private val listenInterval = 3000L
    private val listenInterval = 30L

    // The coroutine job in which BlockchainService listens to the blockchain
    private var listenJob: Job = Job()

    // Web3j web3 instance
    //TODO: Inject this
    private val web3: Web3j = Web3j.build(HttpService("http://192.168.1.13:8545"))

    // Gas price provider
    private val gasProvider = DumbGasProvider()

    // Commuto transaction manager
    private val txManager = CommutoTransactionManager(web3, creds, ChainIdLong.NONE)

    // Commuto Swap contract instance
    private val commutoSwap: CommutoSwap = CommutoSwap.load(
        commutoSwapAddress,
        web3,
        txManager,
        gasProvider
    )

    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        listenJob = GlobalScope.launch {
            listenLoop()
        }
    }

    fun stopListening() {
        listenJob.cancel()
    }

    suspend fun listenLoop() {
        while (true) {
            newestBlockNum = getNewestBlockNumberAsync().await().blockNumber
            if (newestBlockNum > lastParsedBlockNum) {
                val block = getBlockAsync(lastParsedBlockNum + BigInteger.ONE).await()
                    .block
                parseBlock(block)

                setLastParsedBlockNumber(block.number)
            }
            delay(listenInterval)
        }
    }

    private fun getNewestBlockNumberAsync(): Deferred<EthBlockNumber> {
        return web3.ethBlockNumber().sendAsync().asDeferred()
    }

    private fun getBlockAsync(blockNumber: BigInteger): Deferred<EthBlock> {
        return web3.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(blockNumber),
            false
        ).sendAsync().asDeferred()
    }

    private fun getDeferredTxReceiptOptionals (
        txHashes: List<String>
    ): List<Deferred<EthGetTransactionReceipt>> {
        return txHashes.map {
            web3.ethGetTransactionReceipt(it).sendAsync().asDeferred()
        }
    }

    private suspend fun parseBlock(block: EthBlock.Block) {
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
        handleEventResponses(eventResponses)
    }

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

    private fun getEventResponsesFromReceipt(receipt: TransactionReceipt): List<BaseEventResponse> {
        val eventResponses: MutableList<List<BaseEventResponse>> = mutableListOf()
        eventResponses.add(commutoSwap.getOfferOpenedEvents(receipt))
        eventResponses.add(commutoSwap.getOfferTakenEvents(receipt))
        return eventResponses.flatten()
    }

    private fun handleEventResponses(
        eventResponseLists: MutableList<List<BaseEventResponse>>
    ) {
        val eventResponses = eventResponseLists.flatten()
        for (eventResponse in eventResponses) {
            if (eventResponse is CommutoSwap.OfferOpenedEventResponse) {
                offerService.handleOfferOpenedEvent(eventResponse)
            } else if (eventResponse is CommutoSwap.OfferTakenEventResponse) {
                offerService.handleOfferTakenEvent(eventResponse)
            }
        }
    }

}