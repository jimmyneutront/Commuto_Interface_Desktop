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
    fun testListen() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

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
            testingServerClient.get(testingServiceUrl).body()
        }

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
                offerEventResponse:
                CommutoSwap.OfferOpenedEventResponse
            ) {
                runBlocking {
                    offerOpenedEventChannel.send(offerEventResponse)
                }
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
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(30_000) {
                // TODO: check the properties of the received events
                val receivedOpenOfferEvent = offerService.offerOpenedEventChannel.receive()
                val receivedOfferTakenEvent = offerService.offerTakenEventChannel.receive()
            }
        }
    }
}