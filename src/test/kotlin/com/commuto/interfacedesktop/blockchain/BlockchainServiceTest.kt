package com.commuto.interfacedesktop.blockchain

import com.commuto.interfacedesktop.CommutoSwap
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.OfferService
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

class BlockchainServiceTest {
    @Test
    fun testBlockchainService() = runBlocking {
        class TestBlockchainExceptionHandler : BlockchainExceptionNotifiable {
            @Throws
            override fun handleBlockchainException(exception: Exception) {
                throw exception
            }
        }
        val blockchainService = BlockchainService(
            TestBlockchainExceptionHandler(),
            OfferService()
        )
        blockchainService.listenLoop()
    }

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

        val w3 = Web3j.build(HttpService("http://192.168.0.195:8545"))

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
            override fun handleOfferOpenedEvent(
                offerOpenedEventResponse:
                CommutoSwap.OfferOpenedEventResponse
            ) {
                runBlocking {
                    offerOpenedEventChannel.send(offerOpenedEventResponse)
                }
            }
            override fun handleOfferCanceledEvent(
                offerCanceledEventResponse:
                CommutoSwap.OfferCanceledEventResponse) {
                throw IllegalStateException("Should not be called")
            }
            override fun handleOfferTakenEvent(
                offerTakenEventResponse:
                CommutoSwap.OfferTakenEventResponse
            ) {
                runBlocking {
                    offerTakenEventChannel.send(offerTakenEventResponse)
                }
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

        val w3 = Web3j.build(HttpService("http://192.168.0.195:8545"))

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
            override fun handleOfferOpenedEvent(
                offerOpenedEventResponse:
                CommutoSwap.OfferOpenedEventResponse) {
                runBlocking {
                    offerOpenedEventChannel.send(offerOpenedEventResponse)
                }
            }
            override fun handleOfferCanceledEvent(
                offerCanceledEventResponse:
                CommutoSwap.OfferCanceledEventResponse) {
                runBlocking {
                    offerCanceledEventChannel.send(offerCanceledEventResponse)
                }
            }
            override fun handleOfferTakenEvent(
                offerTakenEventResponse:
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
            override fun handleOfferOpenedEvent(
                offerOpenedEventResponse:
                CommutoSwap.OfferOpenedEventResponse
            ) {
                throw IllegalStateException("Should not be called")
            }
            override fun handleOfferCanceledEvent(
                offerCanceledEventResponse:
                CommutoSwap.OfferCanceledEventResponse
            ) {
                throw IllegalStateException("Should not be called")
            }
            override fun handleOfferTakenEvent(
                offerTakenEventResponse:
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