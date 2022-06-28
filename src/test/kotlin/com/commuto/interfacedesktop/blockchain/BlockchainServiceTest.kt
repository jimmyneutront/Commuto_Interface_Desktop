package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.CommutoSwap
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.ui.OffersViewModel
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
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
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
        class TestBlockchainExceptionHandler : BlockchainExceptionNotifiable {
            @Throws
            override fun handleBlockchainException(exception: Exception) {
                throw exception
            }
        }
        val offersService = OfferService()
        OffersViewModel(offersService)
        val blockchainService = BlockchainService(
            TestBlockchainExceptionHandler(),
            offersService
        )
        blockchainService.listenLoop()
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
                requestTimeoutMillis = 90_000
                socketTimeoutMillis = 90_000
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

        val w3 = Web3j.build(HttpService("http://192.168.1.13:8545"))

        class TestBlockchainExceptionHandler : BlockchainExceptionNotifiable {
            @Throws
            override fun handleBlockchainException(exception: Exception) {
                throw exception
            }
        }
        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<CommutoSwap.OfferOpenedEventResponse>()
            val offerTakenEventChannel = Channel<CommutoSwap.OfferTakenEventResponse>()
            override suspend fun handleOfferOpenedEvent(
                event:
                CommutoSwap.OfferOpenedEventResponse
            ) {
                offerOpenedEventChannel.send(event)
            }
            override suspend fun handleOfferCanceledEvent(
                event:
                CommutoSwap.OfferCanceledEventResponse) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(
                event:
                CommutoSwap.OfferTakenEventResponse
            ) {
                offerTakenEventChannel.send(event)
            }
        }

        val offerService = TestOfferService()
        val blockchainService = BlockchainService(
            blockchainExceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(90_000) {
                val receivedOfferOpenedEvent = offerService.offerOpenedEventChannel.receive()
                assert(Arrays.equals(receivedOfferOpenedEvent.offerID, offerIdByteBuffer.array()))
                val receivedOfferTakenEvent = offerService.offerTakenEventChannel.receive()
                assert(Arrays.equals(receivedOfferTakenEvent.offerID, offerIdByteBuffer.array()))
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

        val w3 = Web3j.build(HttpService("http://192.168.1.13:8545"))

        class TestBlockchainExceptionHandler : BlockchainExceptionNotifiable {
            @Throws
            override fun handleBlockchainException(exception: Exception) {
                throw exception
            }
        }
        val blockchainExceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService : OfferNotifiable {
            val offerOpenedEventChannel = Channel<CommutoSwap.OfferOpenedEventResponse>()
            val offerCanceledEventChannel = Channel<CommutoSwap.OfferCanceledEventResponse>()
            override suspend fun handleOfferOpenedEvent(
                event:
                CommutoSwap.OfferOpenedEventResponse) {
                offerOpenedEventChannel.send(event)
            }
            override suspend fun handleOfferCanceledEvent(
                event:
                CommutoSwap.OfferCanceledEventResponse) {
                offerCanceledEventChannel.send(event)
            }
            override suspend fun handleOfferTakenEvent(
                event:
                CommutoSwap.OfferTakenEventResponse) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            blockchainExceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                val receivedOfferOpenedEvent = offerService.offerOpenedEventChannel.receive()
                assert(Arrays.equals(receivedOfferOpenedEvent.offerID, offerIdByteBuffer.array()))
                val receivedOfferCanceledEvent = offerService.offerCanceledEventChannel.receive()
                assert(Arrays.equals(receivedOfferCanceledEvent.offerID, offerIdByteBuffer.array()))
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
            override suspend fun handleOfferOpenedEvent(
                event:
                CommutoSwap.OfferOpenedEventResponse
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferCanceledEvent(
                event:
                CommutoSwap.OfferCanceledEventResponse
            ) {
                throw IllegalStateException("Should not be called")
            }
            override suspend fun handleOfferTakenEvent(
                event:
                CommutoSwap.OfferTakenEventResponse
            ) {
                throw IllegalStateException("Should not be called")
            }
        }
        val offerService = TestOfferService()

        val blockchainService = BlockchainService(
            blockchainExceptionHandler,
            offerService,
            w3,
            "0x0000000000000000000000000000000000000000"
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