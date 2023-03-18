package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.CommutoWeb3j
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.events.erc20.ApprovalEvent
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.dispute.TestDisputeService
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.offer.*
import com.commuto.interfacedesktop.swap.SwapNotifiable
import com.commuto.interfacedesktop.swap.SwapServiceTests
import com.commuto.interfacedesktop.swap.TestSwapService
import com.commuto.interfacedesktop.ui.offer.OffersViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Tests for [BlockchainService]
 */
class BlockchainServiceTest {

    /**
     * Runs [BlockchainService.listenLoop] in the current coroutine context. This doesn't actually
     * test anything.
     */
    @Test
    fun testBlockchainService() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        val offersService = OfferService(
            databaseService = databaseService,
            keyManagerService = KeyManagerService(databaseService),
            swapService = TestSwapService()
        )
        OffersViewModel(offersService)
        val blockchainService = BlockchainService(
            errorHandler = TestBlockchainExceptionHandler(),
            offerService = offersService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
        )
        blockchainService.listenLoop()
    }

    /**
     * Ensures [BlockchainService.getServiceFeeRateAsync] functions properly.
     */
    @Test
    fun testGetServiceFeeRate() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_getServiceFeeRate"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl).body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        runBlocking {
            assertEquals(blockchainService.getServiceFeeRateAsync().await(), BigInteger.valueOf(100L))
        }
    }

    /**
     * [BlockchainService.approveTokenTransferAsync] is  tested by [OfferServiceTests.testOpenOffer].
     */

    /**
     * [BlockchainService.openOfferAsync] is tested by [OfferServiceTests.testOpenOffer].
     */

    /**
     * [BlockchainService.cancelOfferAsync] is tested by [OfferServiceTests.testCancelOffer].
     */

    /**
     * [BlockchainService.fillSwapAsync] is tested by [SwapServiceTests.testFillSwap].
     */

    /**
     * [BlockchainService.reportPaymentSentAsync] is tested by [SwapServiceTests.testReportPaymentSent]
     */

    /**
     * [BlockchainService.reportPaymentReceivedAsync] is tested by [SwapServiceTests.testReportPaymentReceived]
     */

    /**
     * [BlockchainService.closeSwapAsync] is tested by [SwapServiceTests.testCloseSwap]
     */

    /**
     * Ensure [BlockchainService] detects and handles failed monitored transactions properly.
     */
    @Test
    fun testHandleFailedTransaction() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val transactionHash: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_handleFailedTransaction"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-taken")
                }
            }.body()
        }
        val failedTransactionHash = testingServerResponse.transactionHash

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val failedTransactionChannel = Channel<BlockchainTransaction>()
            val transactionFailureExceptionChannel = Channel<BlockchainTransactionException>()
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                failedTransactionChannel.send(transaction)
                transactionFailureExceptionChannel.send(exception)
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.addTransactionToMonitor(
            transaction = BlockchainTransaction(
                transactionHash = failedTransactionHash,
                timeOfCreation = Date(),
                latestBlockNumberAtCreation = BigInteger.ZERO,
                type = BlockchainTransactionType.CANCEL_OFFER
            )
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                val failedTransaction = offerService.failedTransactionChannel.receive()
                offerService.transactionFailureExceptionChannel.receive()
                assertEquals(BlockchainTransactionType.CANCEL_OFFER, failedTransaction.type)
                assertEquals(failedTransactionHash, failedTransaction.transactionHash)
            }
        }
    }

    /**
     * Ensure [BlockchainService] detects and handles monitored transactions that have been dropped or have been pending
     * for too long.
     */
    @Test
    fun testHandleLongPendingOrDroppedTransaction() {

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_handleLongPendingOrDroppedTransaction"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl).body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val failedTransactionChannel = Channel<BlockchainTransaction>()
            val transactionFailureExceptionChannel = Channel<BlockchainTransactionException>()
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                failedTransactionChannel.send(transaction)
                transactionFailureExceptionChannel.send(exception)
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        blockchainService.addTransactionToMonitor(BlockchainTransaction(
            transactionHash = "a_nonexistent_tx_hash",
            timeOfCreation = Date(Date().time - 86_401_000),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.CANCEL_OFFER,
        ))
        blockchainService.listen()
        runBlocking {
            withTimeout(20_000_000_000_000) {
                val failedTransaction = offerService.failedTransactionChannel.receive()
                offerService.transactionFailureExceptionChannel.receive()
                assertEquals(BlockchainTransactionType.CANCEL_OFFER, failedTransaction.type)
                assertEquals("a_nonexistent_tx_hash", failedTransaction.transactionHash)
                assertNull(blockchainService.getMonitoredTransaction("a_nonexistent_tx_hash"))
            }
        }

    }

    /**
     * Tests [BlockchainService] by ensuring it detects and handles
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) and
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events
     * for a specific offer properly.
     */
    @Test
    fun testListenOfferOpenedTaken() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-taken")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        offerIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerTakenEventChannel = Channel<OfferTakenEvent>()
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                offerOpenedEventChannel.send(event)
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                offerTakenEventChannel.send(event)
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerService.offerOpenedEventChannel.receive()
                assertEquals(receivedOfferOpenedEvent.offerID, expectedOfferId)
                val receivedOfferTakenEvent = offerService.offerTakenEventChannel.receive()
                assertEquals(receivedOfferTakenEvent.offerID, expectedOfferId)
            }
        }
    }

    /**
     * Tests [BlockchainService] by ensuring it detects and handles
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) and
     * [OfferCanceled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offercanceled) events
     * for a specific offer properly. This ensures that such events emitted by transactions not made by this interface
     * and those emitted by monitored transactions (those that are made by this interface) are detected and handled
     * properly.
     */
    @Test
    fun testListenOfferOpenedCanceled() {
        @Serializable
        data class TestingServerResponse(
            val commutoSwapAddress: String,
            val offerId: String,
            val offerCancellationTransactionHash: String
        )

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-canceled")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        offerIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)
        val offerCancellationTransactionHash = testingServerResponse.offerCancellationTransactionHash

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerCanceledEventChannel = Channel<OfferCanceledEvent>()
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                offerOpenedEventChannel.send(event)
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                offerCanceledEventChannel.send(event)
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerServiceForNonMonitoredTxns = TestOfferService()

        val blockchainServiceForNonMonitoredTxns = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerServiceForNonMonitoredTxns,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainServiceForNonMonitoredTxns.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerServiceForNonMonitoredTxns.offerOpenedEventChannel.receive()
                assertEquals(receivedOfferOpenedEvent.offerID, expectedOfferId)
                val receivedOfferCanceledEvent = offerServiceForNonMonitoredTxns.offerCanceledEventChannel.receive()
                assertEquals(receivedOfferCanceledEvent.offerID, expectedOfferId)
                assertFalse(blockchainExceptionHandler.gotError)
            }
        }
        blockchainServiceForNonMonitoredTxns.stopListening()

        val offerServiceForMonitoredTxns = TestOfferService()

        val blockchainServiceForMonitoredTxns = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerServiceForMonitoredTxns,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainServiceForMonitoredTxns.addTransactionToMonitor(
            transaction = BlockchainTransaction(
                transactionHash = offerCancellationTransactionHash,
                timeOfCreation = Date(),
                latestBlockNumberAtCreation = BigInteger.ZERO,
                type = BlockchainTransactionType.CANCEL_OFFER
            )
        )
        blockchainServiceForMonitoredTxns.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerServiceForMonitoredTxns.offerOpenedEventChannel.receive()
                assertEquals(receivedOfferOpenedEvent.offerID, expectedOfferId)
                val receivedOfferCanceledEvent = offerServiceForMonitoredTxns.offerCanceledEventChannel.receive()
                assertEquals(receivedOfferCanceledEvent.offerID, expectedOfferId)
                assertFalse(blockchainExceptionHandler.gotError)
                assertNull(blockchainServiceForMonitoredTxns.getMonitoredTransaction(offerCancellationTransactionHash))
            }
        }

    }

    /**
     * Tests [BlockchainService] by ensuring it detects and handles
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) and
     * [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited) events
     * for a specific offer properly.
     */
    @Test
    fun testListenOfferOpenedEdited() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-edited")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        offerIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerEditedEventChannel = Channel<OfferEditedEvent>()
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                offerOpenedEventChannel.send(event)
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                offerEditedEventChannel.send(event)
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerService.offerOpenedEventChannel.receive()
                assertEquals(receivedOfferOpenedEvent.offerID, expectedOfferId)
                val receivedOfferEditedEvent = offerService.offerEditedEventChannel.receive()
                assertEquals(receivedOfferEditedEvent.offerID, expectedOfferId)
            }
        }
    }

    /**
     * Tests [BlockchainService] to ensure it detects and handles
     * [SwapFilled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swapfilled) events properly.
     */
    @Test
    fun testListenSwapFilled() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val swapID: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-taken-swapFilled")
                }
            }.body()
        }
        val expectedSwapID = UUID.fromString(testingServerResponse.swapID)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of SwapFilled events
        class TestSwapService: SwapNotifiable {
            val swapFilledEventChannel = Channel<SwapFilledEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {
                swapFilledEventChannel.send(event)
            }
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
            disputeService = TestDisputeService(),
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val event = swapService.swapFilledEventChannel.receive()
                assertEquals(expectedSwapID, event.swapID)
            }
        }
    }

    /**
     * Tests [BlockchainService] to ensure it detects and handles
     * [PaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#paymentsent) events properly.
     */
    @Test
    fun testListenPaymentSent() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val swapID: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-taken-SwapFilled-PaymentSent")
                }
            }.body()
        }
        val expectedSwapID = UUID.fromString(testingServerResponse.swapID)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of SwapFilled events
        class TestSwapService: SwapNotifiable {
            val paymentSentEventChannel = Channel<PaymentSentEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {
                paymentSentEventChannel.send(event)
            }
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
            disputeService = TestDisputeService(),
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val event = swapService.paymentSentEventChannel.receive()
                assertEquals(expectedSwapID, event.swapID)
            }
        }
    }

    /**
     * Tests [BlockchainService] to ensure it detects and handles
     * [PaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#paymentreceived) events properly.
     */
    @Test
    fun testListenPaymentReceived() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val swapID: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-opened-taken-SwapFilled-PaymentSent-Received")
                }
            }.body()
        }
        val expectedSwapID = UUID.fromString(testingServerResponse.swapID)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of PaymentReceived events
        class TestSwapService: SwapNotifiable {
            val paymentReceivedEventChannel = Channel<PaymentReceivedEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {
                paymentReceivedEventChannel.send(event)
            }
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
            disputeService = TestDisputeService(),
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val event = swapService.paymentReceivedEventChannel.receive()
                assertEquals(expectedSwapID, event.swapID)
            }
        }
    }

    /**
     * Tests [BlockchainService] to ensure it detects and handles
     * [BuyerClosed](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#buyerclosed) events properly.
     */
    @Test
    fun testListenBuyerClosed() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val swapID: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append(
                        "events",
                        "offer-opened-taken-SwapFilled-PaymentSent-Received-BuyerClosed"
                    )
                }
            }.body()
        }
        val expectedSwapID = UUID.fromString(testingServerResponse.swapID)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of BuyerClosed events
        class TestSwapService: SwapNotifiable {
            val buyerClosedEventChannel = Channel<BuyerClosedEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {
                buyerClosedEventChannel.send(event)
            }
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
            disputeService = TestDisputeService(),
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val event = swapService.buyerClosedEventChannel.receive()
                assertEquals(expectedSwapID, event.swapID)
            }
        }
    }

    /**
     * Tests [BlockchainService] to ensure it detects and handles
     * [SellerClosed](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#sellerclosed) events properly.
     */
    @Test
    fun testListenSellerClosed() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val swapID: String)

        val testingServiceUrl = "http://localhost:8546/test_blockchainservice_listen"
        val testingServerClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 90_000
                requestTimeoutMillis = 90_000
            }
        }
        val testingServerResponse: TestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append(
                        "events",
                        "offer-opened-taken-SwapFilled-PaymentSent-Received-SellerClosed"
                    )
                }
            }.body()
        }
        val expectedSwapID = UUID.fromString(testingServerResponse.swapID)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of BuyerClosed events
        class TestSwapService: SwapNotifiable {
            val sellerClosedEventChannel = Channel<SellerClosedEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {
                sellerClosedEventChannel.send(event)
            }
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
            disputeService = TestDisputeService(),
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val event = swapService.sellerClosedEventChannel.receive()
                assertEquals(expectedSwapID, event.swapID)
            }
        }
    }

    /**
     * Tests [BlockchainService]'s [Exception] handling logic by ensuring that it handles empty node
     * response exceptions properly.
     */
    @Test
    fun testListenErrorHandling() {
        val w3 = CommutoWeb3j(HttpService("http://not.a.node:8546"))
        // We need this TestBlockchainExceptionHandler to test exception handling logic
        class TestBlockchainExceptionHandler : BlockchainExceptionNotifiable {
            val exceptionChannel = Channel<Exception>()
            override fun handleBlockchainException(exception: Exception) {
                runBlocking {
                    exceptionChannel.send(exception)
                }
            }
        }
        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            disputeService = TestDisputeService(),
            web3 = w3,
            commutoSwapAddress = "0x0000000000000000000000000000000000000000"
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(10_000) {
                val exception = blockchainExceptionHandler.exceptionChannel.receive()
                assert(exception is UnknownHostException)
                assert(exception.message ==
                        "not.a.node: nodename nor servname provided, or not known")
            }
        }
    }
}