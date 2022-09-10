package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.offer.OfferServiceTests
import com.commuto.interfacedesktop.offer.TestOfferService
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
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.*

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

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
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

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerTakenEventChannel = Channel<OfferTakenEvent>()
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
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events
     * for a specific offer properly.
     */
    @Test
    fun testListenOfferOpenedCanceled() {
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
                    parameters.append("events", "offer-opened-canceled")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        offerIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerCanceledEventChannel = Channel<OfferCanceledEvent>()
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
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            exceptionHandler = blockchainExceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerService.offerOpenedEventChannel.receive()
                assertEquals(receivedOfferOpenedEvent.offerID, expectedOfferId)
                val receivedOfferCanceledEvent = offerService.offerCanceledEventChannel.receive()
                assertEquals(receivedOfferCanceledEvent.offerID, expectedOfferId)
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

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<OfferOpenedEvent>()
            val offerEditedEventChannel = Channel<OfferEditedEvent>()
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

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of SwapFilled events
        class TestSwapService: SwapNotifiable {
            val swapFilledEventChannel = Channel<SwapFilledEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleNewSwap(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {
                swapFilledEventChannel.send(event)
            }
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
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

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val exceptionHandler = TestBlockchainExceptionHandler()

        // We need this TestSwapService to track handling of SwapFilled events
        class TestSwapService: SwapNotifiable {
            val paymentSentEventChannel = Channel<PaymentSentEvent>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleNewSwap(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {
                paymentSentEventChannel.send(event)
            }
        }
        val swapService = TestSwapService()

        val blockchainService = BlockchainService(
            exceptionHandler,
            TestOfferService(),
            swapService,
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
     * Tests [BlockchainService]'s [Exception] handling logic by ensuring that it handles empty node
     * response exceptions properly.
     */
    @Test
    fun testListenErrorHandling() {
        val w3 = Web3j.build(HttpService("http://not.a.node:8546"))
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