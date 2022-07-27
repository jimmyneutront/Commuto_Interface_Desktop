package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiable
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.validation.validateNewOfferData
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.ui.PreviewableOfferTruthSource
import com.commuto.interfacedesktop.ui.StablecoinInformation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Tests for [OfferService]
 */
class OfferServiceTests {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) events properly.
     */
    @Test
    fun testHandleOfferOpenedEvent()  {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_handleOfferOpenedEvent"
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
                    parameters.append("events", "offer-opened")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val expectedOfferIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        expectedOfferIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        expectedOfferIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)
        val expectedOfferIdByteArray = expectedOfferIdByteBuffer.array()

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferOpenedEvent>() {

            var appendedEvent: OfferOpenedEvent? = null
            var removedEvent: OfferOpenedEvent? = null

            override fun append(element: OfferOpenedEvent) {
                appendedEvent = element
                super.append(element)
            }

            override fun remove(elementToRemove: OfferOpenedEvent) {
                removedEvent = elementToRemove
                super.remove(elementToRemove)
            }

        }
        val offerOpenedEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            offerOpenedEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            val offersChannel = Channel<Offer>()
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            override fun addOffer(offer: Offer) {
                offers[offer.id] = offer
                runBlocking {
                    offersChannel.send(offer)
                }
            }

            override fun removeOffer(id: UUID) {
                throw IllegalStateException("TestOfferTruthSource.removeOffer should not be called here")
            }
        }
        val offerTruthSource = TestOfferTruthSource()


        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        val encoder = Base64.getEncoder()
        runBlocking {
            withTimeout(60_000) {
                offerTruthSource.offersChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assert(offerTruthSource.offers.size == 1)
                assertEquals(offerTruthSource.offers[expectedOfferId]!!.id, expectedOfferId)
                assertEquals(offerOpenedEventRepository.appendedEvent!!.offerID, expectedOfferId)
                assertEquals(offerOpenedEventRepository.removedEvent!!.offerID, expectedOfferId)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assert(offerInDatabase!!.isCreated == 1L)
                assert(offerInDatabase.isTaken == 0L)
                assertEquals(offerInDatabase.interfaceId, encoder.encodeToString("maker interface Id here"
                    .encodeToByteArray()))
                assertEquals(offerInDatabase.amountLowerBound, "10000")
                assertEquals(offerInDatabase.amountUpperBound, "10000")
                assertEquals(offerInDatabase.securityDepositAmount, "1000")
                assertEquals(offerInDatabase.serviceFeeRate, "100")
                assertEquals(offerInDatabase.onChainDirection, "1")
                assertEquals(offerInDatabase.protocolVersion, "1")
                val settlementMethodsInDatabase = databaseService.getSettlementMethods(encoder
                    .encodeToString(expectedOfferIdByteArray), offerInDatabase.chainID)
                assertEquals(settlementMethodsInDatabase!!.size, 1)
                assertEquals(settlementMethodsInDatabase[0], encoder.encodeToString("USD-SWIFT|a price here"
                    .encodeToByteArray()))
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferCanceled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offercanceled) events properly.
     */
    @Test
    fun testHandleOfferCanceledEvent() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_handleOfferCanceledEvent"
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
                    parameters.append("events", "offer-opened")
                }
            }.body()
        }
        val expectedOfferId = UUID.fromString(testingServerResponse.offerId)
        val expectedOfferIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        expectedOfferIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        expectedOfferIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)
        val expectedOfferIdByteArray = expectedOfferIdByteBuffer.array()

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferCanceledEvent>() {

            var appendedEvent: OfferCanceledEvent? = null
            var removedEvent: OfferCanceledEvent? = null

            override fun append(element: OfferCanceledEvent) {
                appendedEvent = element
                super.append(element)
            }

            override fun remove(elementToRemove: OfferCanceledEvent) {
                removedEvent = elementToRemove
                super.remove(elementToRemove)
            }

        }
        val offerCanceledEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            offerCanceledEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            val offersChannel = Channel<Offer>()
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            override fun addOffer(offer: Offer) {
                offers[offer.id] = offer
                runBlocking {
                    offersChannel.send(offer)
                }
            }
            override fun removeOffer(id: UUID) {
                val offerToSend = offers[id]!!
                offers.remove(id)
                runBlocking {
                    offersChannel.send(offerToSend)
                }
            }
        }
        val offerTruthSource = TestOfferTruthSource()

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        runBlocking {
            withTimeout(60_000) {
                val addedOffer = offerTruthSource.offersChannel.receive()
                assertNotNull(offerTruthSource.offers[addedOffer.id])
            }
        }

        runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-canceled")
                    parameters.append("commutoSwapAddress", testingServerResponse.commutoSwapAddress)
                }
            }
        }

        runBlocking {
            withTimeout(20_000) {
                val encoder = Base64.getEncoder()
                offerTruthSource.offersChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assert(offerTruthSource.offers.isEmpty())
                assertEquals(offerCanceledEventRepository.appendedEvent!!.offerID, expectedOfferId)
                assertEquals(offerCanceledEventRepository.removedEvent!!.offerID, expectedOfferId)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assertEquals(offerInDatabase, null)
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events properly.
     */
    @Test
    fun testHandleOfferTakenEvent() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_handleOfferTakenEvent"
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
        val expectedOfferIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        expectedOfferIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        expectedOfferIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)
        val expectedOfferIdByteArray = expectedOfferIdByteBuffer.array()

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferTakenEvent>() {

            var appendedEvent: OfferTakenEvent? = null
            var removedEvent: OfferTakenEvent? = null

            override fun append(element: OfferTakenEvent) {
                appendedEvent = element
                super.append(element)
            }

            override fun remove(elementToRemove: OfferTakenEvent) {
                removedEvent = elementToRemove
                super.remove(elementToRemove)
            }

        }
        val offerTakenEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            offerTakenEventRepository,
            BlockchainEventRepository(),
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            val offersChannel = Channel<Offer>()
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            override fun addOffer(offer: Offer) {
                offers[offer.id] = offer
            }
            override fun removeOffer(id: UUID) {
                val offerToSend = offers[id]!!
                offers.remove(id)
                runBlocking {
                    offersChannel.send(offerToSend)
                }
            }
        }
        val offerTruthSource = TestOfferTruthSource()

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        val encoder = Base64.getEncoder()
        runBlocking {
            withTimeout(60_000) {
                offerTruthSource.offersChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assert(offerTruthSource.offers.isEmpty())
                assertEquals(offerTakenEventRepository.appendedEvent!!.offerID, expectedOfferId)
                assertEquals(offerTakenEventRepository.removedEvent!!.offerID, expectedOfferId)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assertEquals(offerInDatabase, null)
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited) events properly.
     */
    @Test
    fun handleOfferEditedEvent() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val offerId: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_handleOfferEditedEvent"
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
        val expectedOfferIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
        expectedOfferIdByteBuffer.putLong(expectedOfferId.mostSignificantBits)
        expectedOfferIdByteBuffer.putLong(expectedOfferId.leastSignificantBits)
        val expectedOfferIdByteArray = expectedOfferIdByteBuffer.array()

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferEditedEvent>() {

            var appendedEvent: OfferEditedEvent? = null
            var removedEvent: OfferEditedEvent? = null

            override fun append(element: OfferEditedEvent) {
                appendedEvent = element
                super.append(element)
            }

            override fun remove(elementToRemove: OfferEditedEvent) {
                removedEvent = elementToRemove
                super.remove(elementToRemove)
            }

        }
        val offerEditedEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            BlockchainEventRepository(),
            offerEditedEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            val offersChannel = Channel<Offer>()
            var offersAddedCounter = 0
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            override fun addOffer(offer: Offer) {
                offersAddedCounter++
                offers[offer.id] = offer
                if (offersAddedCounter == 2) {
                    // We only want to send the offer with the updated price, not the first one.
                    runBlocking {
                        offersChannel.send(offer)
                    }
                }
            }
            override fun removeOffer(id: UUID) {
                offers.remove(id)
            }
        }
        val offerTruthSource = TestOfferTruthSource()

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        val encoder = Base64.getEncoder()
        runBlocking {
            withTimeout(60_000) {
                offerTruthSource.offersChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assert(offerTruthSource.offers.size == 1)
                assertEquals(offerTruthSource.offers[expectedOfferId]!!.id, expectedOfferId)
                assertEquals(offerEditedEventRepository.appendedEvent!!.offerID, expectedOfferId)
                assertEquals(offerEditedEventRepository.removedEvent!!.offerID, expectedOfferId)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assert(offerInDatabase!!.isCreated == 1L)
                assert(offerInDatabase.isTaken == 0L)
                assertEquals(offerInDatabase.interfaceId, encoder.encodeToString("maker interface Id here"
                    .encodeToByteArray()))
                assertEquals(offerInDatabase.amountLowerBound, "10000")
                assertEquals(offerInDatabase.amountUpperBound, "10000")
                assertEquals(offerInDatabase.securityDepositAmount, "1000")
                assertEquals(offerInDatabase.serviceFeeRate, "100")
                assertEquals(offerInDatabase.onChainDirection, "1")
                assertEquals(offerInDatabase.protocolVersion, "1")
                val settlementMethodsInDatabase = databaseService.getSettlementMethods(encoder
                    .encodeToString(expectedOfferIdByteArray), offerInDatabase.chainID)
                assertEquals(settlementMethodsInDatabase!!.size, 1)
                assertEquals(settlementMethodsInDatabase[0], encoder.encodeToString("EUR-SEPA|an edited price here"
                    .encodeToByteArray()))
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [Public Key Announcements](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * properly.
     */
    @Test
    fun testHandlePublicKeyAnnouncement() {

        class TestDatabaseService: DatabaseService(DatabaseDriverFactory()) {
            val updatedOfferIDChannel = Channel<String>()
            override suspend fun updateOfferHavePublicKey(offerID: String, chainID: String, havePublicKey: Boolean) {
                super.updateOfferHavePublicKey(offerID, chainID, havePublicKey)
                updatedOfferIDChannel.send(offerID)
            }
        }

        val databaseService = TestDatabaseService()
        databaseService.createTables()
        val keyManagerService =  KeyManagerService(databaseService)

        val offerService = OfferService(databaseService, keyManagerService)

        val offerTruthSource = PreviewableOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val keyPair = runBlocking {
            keyManagerService.generateKeyPair(false)
        }
        val publicKey = PublicKey(keyPair.keyPair.public)

        val offerID = UUID.randomUUID()
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = publicKey.interfaceId,
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ONE,
            securityDepositAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            onChainDirection = BigInteger.ZERO,
            onChainSettlementMethods = listOf(),
            protocolVersion = BigInteger.ZERO,
            chainID = BigInteger.ZERO,
            havePublicKey = false
        )
        offerTruthSource.offers[offerID] = offer
        val isCreated = if (offer.isCreated) 1L else 0L
        val isTaken = if (offer.isTaken) 1L else 0L
        val havePublicKey = if (offer.havePublicKey) 1L else 0L
        val encoder = Base64.getEncoder()
        val offerForDatabase = com.commuto.interfacedesktop.db.Offer(
            isCreated = isCreated,
            isTaken = isTaken,
            offerId = encoder.encodeToString(offerIDByteArray),
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = havePublicKey,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)
        }

        val publicKeyAnnouncement = PublicKeyAnnouncement(offerID, publicKey)

        runBlocking {
            launch {
                offerService.handlePublicKeyAnnouncement(publicKeyAnnouncement)
            }
            launch {
                withTimeout(20_000) {
                    databaseService.updatedOfferIDChannel.receive()
                    assert(offerTruthSource.offers[offerID]!!.havePublicKey)
                    val offerInDatabase = databaseService.getOffer(encoder.encodeToString(offerIDByteArray))
                    assertEquals(offerInDatabase!!.havePublicKey, 1L)
                    val keyInDatabase = keyManagerService.getPublicKey(publicKey.interfaceId)
                    assertEquals(publicKey.publicKey, keyInDatabase!!.publicKey)
                }
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [ServiceFeeRateChanged](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#servicefeeratechanged)
     * events properly.
     */
    @Test
    fun handleServiceFeeRateChangedEvent() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_handleServiceFeeRateChangedEvent"
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
                    parameters.append("events", "ServiceFeeRateChanged")
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<ServiceFeeRateChangedEvent>() {
            var removedEventChannel = Channel<ServiceFeeRateChangedEvent>()
            override fun remove(elementToRemove: ServiceFeeRateChangedEvent) {
                runBlocking {
                    removedEventChannel.send(elementToRemove)
                }
            }
        }
        val serviceFeeRateChangedEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            serviceFeeRateChangedEventRepository,
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            override fun addOffer(offer: Offer) {}
            override fun removeOffer(id: UUID) {}
        }
        val offerTruthSource = TestOfferTruthSource()

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        runBlocking {
            withTimeout(60_000) {
                val serviceFeeRateChangedEvent = serviceFeeRateChangedEventRepository.removedEventChannel.receive()
                assertEquals(serviceFeeRateChangedEvent.newServiceFeeRate, BigInteger.valueOf(200L))
                assertEquals(offerTruthSource.serviceFeeRate.value, BigInteger.valueOf(200L))
            }
        }

    }

    /**
     * Ensures that [OfferService.openOffer] functions properly.
     */
    @Test
    fun testOpenOffer() {
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_openOffer"
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

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        class TestOfferTruthSource: OfferTruthSource {
            init {
                offerService.setOfferTruthSource(this)
            }
            override var offers = mutableStateMapOf<UUID, Offer>()
            override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
            val addedOfferChannel = Channel<Offer>()
            override fun addOffer(offer: Offer) {
                offers[offer.id] = offer
                runBlocking {
                    addedOfferChannel.send(offer)
                }
            }
            override fun removeOffer(id: UUID) {}
        }
        val offerTruthSource = TestOfferTruthSource()

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler,
            offerService,
            w3,
            testingServerResponse.commutoSwapAddress
        )

        val offerSettlementMethods = listOf(SettlementMethod(
            currency = "FIAT",
            price = "1.00",
            method = "Bank Transfer"
        ))
        val offerData = validateNewOfferData(
            stablecoin = testingServerResponse.stablecoinAddress,
            stablecoinInformation = StablecoinInformation(
                currencyCode = "STBL",
                name = "Basic Stablecoin",
                decimal = 3
            ),
            minimumAmount = BigDecimal("100.00"),
            maximumAmount = BigDecimal("200.00"),
            securityDepositAmount = BigDecimal("20.00"),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction =  OfferDirection.SELL,
            settlementMethods = offerSettlementMethods
        )
        runBlocking {
            launch {
                offerService.openOffer(offerData)
            }
            launch {
                withTimeout(60_000) {
                    val addedOffer = offerTruthSource.addedOfferChannel.receive()
                    val addedOfferID = offerTruthSource.offers.keys.first()
                    assert(addedOffer.isCreated)
                    assertFalse(addedOffer.isTaken)
                    assertEquals(addedOffer.id, addedOfferID)
                    //TOOD: check proper maker address once WalletService is implemented
                    assertEquals(addedOffer.stablecoin, testingServerResponse.stablecoinAddress)
                    assertEquals(addedOffer.amountLowerBound, BigInteger.valueOf(100_000))
                    assertEquals(addedOffer.amountUpperBound, BigInteger.valueOf(200_000))
                    assertEquals(addedOffer.securityDepositAmount, BigInteger.valueOf(20_000))
                    assertEquals(addedOffer.serviceFeeRate, BigInteger.valueOf(100))
                    assertEquals(addedOffer.serviceFeeAmountLowerBound, BigInteger.valueOf(1_000))
                    assertEquals(addedOffer.serviceFeeAmountUpperBound, BigInteger.valueOf(2_000))
                    assertEquals(addedOffer.onChainDirection, BigInteger.ONE)
                    assertEquals(addedOffer.direction, OfferDirection.SELL)
                    assertEquals(addedOffer.onChainSettlementMethods.size, offerSettlementMethods.size)
                    for (index in addedOffer.onChainSettlementMethods.indices) {
                        assert(
                            addedOffer.onChainSettlementMethods[index].contentEquals(
                                Json.encodeToString(offerSettlementMethods[index]).encodeToByteArray()
                            )
                        )
                    }
                    assertEquals(addedOffer.settlementMethods.size, offerSettlementMethods.size)
                    for (index in addedOffer.settlementMethods.indices) {
                        assertEquals(addedOffer.settlementMethods[index], offerSettlementMethods[index])
                    }
                    assertEquals(addedOffer.protocolVersion, BigInteger.ZERO)
                    assert(addedOffer.havePublicKey)

                    assertNotNull(keyManagerService.getKeyPair(addedOffer.interfaceId))

                    val encoder = Base64.getEncoder()
                    val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
                    offerIDByteBuffer.putLong(addedOffer.id.mostSignificantBits)
                    offerIDByteBuffer.putLong(addedOffer.id.leastSignificantBits)
                    val offerIDByteArray = offerIDByteBuffer.array()
                    val offerInDatabase = databaseService.getOffer(encoder.encodeToString(offerIDByteArray))
                    //TODO: check proper maker address once WalletService is implemented
                    val expectedOfferInDatabase = DatabaseOffer(
                        offerId = encoder.encodeToString(offerIDByteArray),
                        isCreated = 1L,
                        isTaken = 0L,
                        maker = "0x0000000000000000000000000000000000000000",
                        interfaceId = encoder.encodeToString(addedOffer.interfaceId),
                        stablecoin = addedOffer.stablecoin,
                        amountLowerBound = addedOffer.amountLowerBound.toString(),
                        amountUpperBound = addedOffer.amountUpperBound.toString(),
                        securityDepositAmount = addedOffer.securityDepositAmount.toString(),
                        serviceFeeRate = addedOffer.serviceFeeRate.toString(),
                        onChainDirection = addedOffer.onChainDirection.toString(),
                        protocolVersion = addedOffer.protocolVersion.toString(),
                        chainID = addedOffer.chainID.toString(),
                        havePublicKey = 1L
                    )
                    assertEquals(expectedOfferInDatabase, offerInDatabase)

                    val offerStructOnChain = blockchainService.getOffer(addedOfferID)!!
                    assert(offerStructOnChain.isCreated)
                    assertFalse(offerStructOnChain.isTaken)
                    // TODO: check proper maker address once WalletService is implemented
                    assert(addedOffer.interfaceId.contentEquals(offerStructOnChain.interfaceID))
                    assertEquals(addedOffer.stablecoin.lowercase(), offerStructOnChain.stablecoin.lowercase())
                    assertEquals(addedOffer.amountLowerBound, offerStructOnChain.amountLowerBound)
                    assertEquals(addedOffer.amountUpperBound, offerStructOnChain.amountUpperBound)
                    assertEquals(addedOffer.securityDepositAmount, offerStructOnChain.securityDepositAmount)
                    assertEquals(addedOffer.serviceFeeRate, offerStructOnChain.serviceFeeRate)
                    assertEquals(addedOffer.onChainDirection, offerStructOnChain.direction)
                    assertEquals(addedOffer.onChainSettlementMethods.size, offerStructOnChain.settlementMethods.size)
                    for (index in addedOffer.onChainSettlementMethods.indices) {
                        assert(
                            addedOffer.onChainSettlementMethods[index].contentEquals(
                                offerStructOnChain.settlementMethods[index]
                            )
                        )
                    }
                    assertEquals(addedOffer.protocolVersion, offerStructOnChain.protocolVersion)
                    // TODO: re-enable this once we get accurate chain IDs
                }
            }
        }

    }

}