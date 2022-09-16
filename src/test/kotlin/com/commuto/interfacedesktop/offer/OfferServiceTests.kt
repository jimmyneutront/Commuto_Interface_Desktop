package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.blockchain.BlockchainEventRepository
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.TestBlockchainExceptionHandler
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.validation.ValidatedNewSwapData
import com.commuto.interfacedesktop.offer.validation.validateNewOfferData
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.TestP2PExceptionHandler
import com.commuto.interfacedesktop.p2p.TestSwapMessageNotifiable
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.swap.SwapNotifiable
import com.commuto.interfacedesktop.swap.SwapState
import com.commuto.interfacedesktop.swap.TestSwapService
import com.commuto.interfacedesktop.swap.TestSwapTruthSource
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.ui.offer.PreviewableOfferTruthSource
import com.commuto.interfacedesktop.ui.StablecoinInformation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import org.junit.Assert.*
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
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) events properly for
     * offers NOT made by the interface user.
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
            TestSwapService(),
            offerOpenedEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        // We need this TestOfferTruthSource to track added offers
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

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
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
                assertEquals(offerInDatabase.state, OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT.asString)
                val settlementMethodsInDatabase = databaseService.getSettlementMethods(encoder
                    .encodeToString(expectedOfferIdByteArray), offerInDatabase.chainID)
                assertEquals(settlementMethodsInDatabase!!.size, 1)
                assertEquals(encoder.encodeToString("{\"f\":\"USD\",\"p\":\"SWIFT\",\"m\":\"1.00\"}"
                        .encodeToByteArray()), settlementMethodsInDatabase[0])
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) events properly for
     * offers made by the interface user.
     */
    @Test
    fun testHandleOfferOpenedEventForUserIsMakerOffer() = runBlocking {

        val newOfferID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val keyPairForOffer = keyManagerService.generateKeyPair(storeResult = true)

        val encoder = Base64.getEncoder()
        val interfaceIDString = encoder.encodeToString(keyPairForOffer.interfaceId)

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_forUserIsMakerOffer_handleOfferOpenedEvent"
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
                    parameters.append("offerID", newOfferID.toString())
                    parameters.append("interfaceID", interfaceIDString)
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferOpenedEvent>() {
            val removedEventChannel = Channel<OfferOpenedEvent>()
            override fun remove(elementToRemove: OfferOpenedEvent) {
                runBlocking {
                    removedEventChannel.send(elementToRemove)
                }
                super.remove(elementToRemove)
            }
        }
        val offerOpenedEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
            offerOpenedEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val p2pExceptionHandler = TestP2PExceptionHandler()

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }

        class TestP2PService: P2PService(
            exceptionHandler = p2pExceptionHandler,
            offerService = offerService,
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var offerIDForAnnouncement: UUID? = null
            var keyPairForAnnouncement: KeyPair? = null
            override suspend fun announcePublicKey(offerID: UUID, keyPair: KeyPair) {
                offerIDForAnnouncement = offerID
                keyPairForAnnouncement = keyPair
            }
        }
        val p2pService = TestP2PService()

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = newOfferID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = keyPairForOffer.interfaceId,
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(),
            protocolVersion = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OPEN_OFFER_TRANSACTION_BROADCAST
        )
        offerTruthSource.addOffer(offer)
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(newOfferID.mostSignificantBits)
        offerIDByteBuffer.putLong(newOfferID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val offerIDString = encoder.encodeToString(offerIDByteArray)
        val offerForDatabase = DatabaseOffer(
            id = offerIDString,
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.direction.string,
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
        )
        databaseService.storeOffer(offerForDatabase)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        runBlocking {
            withTimeout(60_000) {
                offerOpenedEventRepository.removedEventChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assertEquals(p2pService.offerIDForAnnouncement, newOfferID)
                assert(p2pService.keyPairForAnnouncement!!.interfaceId.contentEquals(keyPairForOffer.interfaceId))
                assertEquals(offerTruthSource.offers[newOfferID]!!.state, OfferState.OFFER_OPENED)
                val offerInDatabase = databaseService.getOffer(offerIDString)
                assertEquals(offerInDatabase!!.state, OfferState.OFFER_OPENED.asString)
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
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            offerCanceledEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        // We need this TestOfferTruthSource in order to track added and removed offers
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

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        runBlocking {
            withTimeout(60_000) {
                val addedOffer = offerTruthSource.offersChannel.receive()
                assertNotNull(offerTruthSource.offers[addedOffer.id])
            }
        }

        val openedOffer = offerTruthSource.offers[expectedOfferId]

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
                assertFalse(openedOffer!!.isCreated.value)
                assertEquals(OfferState.CANCELED, openedOffer.state)
                assertEquals(offerCanceledEventRepository.appendedEvent!!.offerID, expectedOfferId)
                assertEquals(offerCanceledEventRepository.removedEvent!!.offerID, expectedOfferId)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assertEquals(offerInDatabase, null)
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events properly for offers
     * neither made nor taken by the interface user.
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
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            offerTakenEventRepository,
            BlockchainEventRepository(),
        )

        // We need this TestOfferTruthSource in order to track removed offers
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

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
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
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events properly for offers
     * taken by the interface user.
     */
    @Test
    fun testHandleOfferTakenEventForUserIsTaker() = runBlocking {

        val newOfferID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        /*
        The public key of this key pair will be the maker's public key (NOT the user's/taker's), so we do NOT want to
        store the whole key pair persistently
         */
        val makerKeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val makerInterfaceID = makerKeyPair.interfaceId
        // We store the maker's public key, as if we received it via the P2P network
        keyManagerService.storePublicKey(makerKeyPair.getPublicKey())

        // This is the taker's (user's) key pair, so we do want to store it persistently
        val takerKeyPair = keyManagerService.generateKeyPair(storeResult = true)

        val encoder = Base64.getEncoder()
        val makerInterfaceIDString = encoder.encodeToString(makerInterfaceID)
        val takerInterfaceIDString = encoder.encodeToString(takerKeyPair.interfaceId)

        // Make testing server set up the proper test environment
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_forUserIsTaker_handleOfferTakenEvent"
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
                    parameters.append("offerID", newOfferID.toString())
                    parameters.append("makerInterfaceID", makerInterfaceIDString)
                    parameters.append("takerInterfaceID", takerInterfaceIDString)
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        /*
        OfferService should call the sendTakerInformation method of this class, passing a UUID equal to newOfferID
        to begin the process of sending taker information
         */
        class TestSwapService: SwapNotifiable {
            val swapIDChannel = Channel<UUID>()
            val chainIDChannel = Channel<BigInteger>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {
                swapIDChannel.send(swapID)
                chainIDChannel.send(chainID)
            }
            override suspend fun handleNewSwap(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            swapService,
        )
        offerService.setOfferTruthSource(TestOfferTruthSource())

        /*
        We have to persistently store a swap with an ID equal to newOfferID and with the maker and taker interface IDs
        created above, otherwise OfferService won't be able to tell that the offer corresponding to the emitted
        OfferTaken event was taken by the user of this interface, and SwapService won't be able to get the keys
        necessary to make the taker info message
         */
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(newOfferID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = "",
            makerInterfaceID = makerInterfaceIDString,
            taker = "",
            takerInterfaceID = takerInterfaceIDString,
            stablecoin = "",
            amountLowerBound = "",
            amountUpperBound = "",
            securityDepositAmount = "",
            takenSwapAmount = "",
            serviceFeeAmount = "",
            serviceFeeRate = "",
            onChainDirection = "",
            settlementMethod = "",
            protocolVersion = "",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "",
            chainID = "",
            state = "",
            role = "",
        )
        databaseService.storeSwap(swapForDatabase)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        withTimeout(60_000) {
            val receivedSwapID = swapService.swapIDChannel.receive()
            val receivedChainID = swapService.chainIDChannel.receive()
            assertEquals(newOfferID, receivedSwapID)
            assertEquals(BigInteger.valueOf(31337L), receivedChainID)
            assertFalse(exceptionHandler.gotError)
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken) events properly for offers
     * made by the interface user.
     */
    @Test
    fun testHandleOfferTakenEventForUserIsMaker() = runBlocking {
        val newOfferID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val keyPair = keyManagerService.generateKeyPair(storeResult = true)

        // Make testing server set up the proper test environment
        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val encoder = Base64.getEncoder()
        val testingServiceUrl = "http://localhost:8546/test_offerservice_forUserIsMaker_handleOfferTakenEvent"
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
                    parameters.append("offerID", newOfferID.toString())
                    parameters.append("interfaceID", encoder.encodeToString(keyPair.interfaceId))
                }
            }.body()
        }

        // Set up node connection
        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val offerTruthSource = TestOfferTruthSource()

        // OfferService should call the handleNewSwap method of this class, passing a UUID equal to offerID
        class TestSwapService: SwapNotifiable {
            val swapIDChannel = Channel<UUID>()
            val chainIDChannel = Channel<BigInteger>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {}
            override suspend fun handleNewSwap(swapID: UUID, chainID: BigInteger) {
                swapIDChannel.send(swapID)
                chainIDChannel.send(chainID)
            }
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            swapService,
        )
        offerService.setOfferTruthSource(offerTruthSource)

        val p2pExceptionHandler = TestP2PExceptionHandler()
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }
        P2PService(
            exceptionHandler = p2pExceptionHandler,
            offerService = offerService,
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        )
        /*
        We have to persistently store an offer with an ID equal to offerID and isUserMaker set to true, so that
        OfferService calls handleNewSwap
         */
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = newOfferID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = keyPair.interfaceId,
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(),
            protocolVersion = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OPEN_OFFER_TRANSACTION_BROADCAST
        )
        offerTruthSource.addOffer(offer)
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(newOfferID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.direction.string,
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
        )
        databaseService.storeOffer(offerForDatabase)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        withTimeout(60_000) {
            val receivedSwapID = swapService.swapIDChannel.receive()
            val receivedChainID = swapService.chainIDChannel.receive()
            assertEquals(newOfferID, receivedSwapID)
            assertEquals(BigInteger.valueOf(31337L), receivedChainID)
            assertFalse(exceptionHandler.gotError)
        }

    }

    /**
     * Ensures that [OfferService] handles
     * [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited) events properly.
     */
    @Test
    fun testHandleOfferEditedEvent() {
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

            val removedEventChannel = Channel<OfferEditedEvent>()

            override fun append(element: OfferEditedEvent) {
                appendedEvent = element
                super.append(element)
            }

            override fun remove(elementToRemove: OfferEditedEvent) {
                removedEvent = elementToRemove
                runBlocking {
                    removedEventChannel.send(elementToRemove)
                }
                super.remove(elementToRemove)
            }

        }
        val offerEditedEventRepository = TestBlockchainEventRepository()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
            BlockchainEventRepository(),
            offerEditedEventRepository,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()
        val encoder = Base64.getEncoder()
        runBlocking {
            withTimeout(60_000) {
                offerEditedEventRepository.removedEventChannel.receive()
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
                assertEquals(offerInDatabase.havePublicKey, 0L)
                assertEquals(offerInDatabase.isUserMaker, 0L)
                assertEquals(offerInDatabase.state, OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT.asString)
                val settlementMethodsInDatabase = databaseService.getSettlementMethods(encoder
                    .encodeToString(expectedOfferIdByteArray), offerInDatabase.chainID)
                assertEquals(settlementMethodsInDatabase!!.size, 1)
                assertEquals(settlementMethodsInDatabase[0], encoder.encodeToString(("{\"f\":\"EUR\",\"p\":\"SEPA\"," +
                        "\"m\":\"0.98\"}").encodeToByteArray()))
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

        val offerService = OfferService(databaseService, keyManagerService, TestSwapService())

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
        val offer = Offer.fromOnChainData(
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
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = false,
            isUserMaker = false,
            state = OfferState.AWAITING_PUBLIC_KEY_ANNOUNCEMENT,
        )
        offerTruthSource.offers[offerID] = offer
        val isCreated = if (offer.isCreated.value) 1L else 0L
        val isTaken = if (offer.isTaken.value) 1L else 0L
        val havePublicKey = if (offer.havePublicKey) 1L else 0L
        val isUserMaker = if (offer.isUserMaker) 1L else 0L
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            isCreated = isCreated,
            isTaken = isTaken,
            id = encoder.encodeToString(offerIDByteArray),
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
            isUserMaker = isUserMaker,
            state = offer.state.asString,
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
                    assertEquals(offerInDatabase.state, "offerOpened")
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
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            serviceFeeRateChangedEventRepository,
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
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
     * Ensures that [OfferService.openOffer], [BlockchainService.approveTokenTransferAsync] and
     * [BlockchainService.openOfferAsync] function properly.
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
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        // We need this TestOfferTruthSource to track added offers
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

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
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
                    assert(addedOffer.isCreated.value)
                    assertFalse(addedOffer.isTaken.value)
                    assertEquals(addedOffer.id, addedOfferID)
                    // TODO: check proper maker address once WalletService is implemented
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
                        id = encoder.encodeToString(offerIDByteArray),
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
                        havePublicKey = 1L,
                        isUserMaker = 1L,
                        state = "openOfferTxPublished"
                    )
                    assertEquals(expectedOfferInDatabase, offerInDatabase)

                    val settlementMethodsInDatabase = databaseService.getSettlementMethods(
                        expectedOfferInDatabase.id, expectedOfferInDatabase.chainID)
                    val expectedSettlementMethodsInDatabase = addedOffer.onChainSettlementMethods.map {
                        encoder.encodeToString(it)
                    }
                    assertEquals(settlementMethodsInDatabase, expectedSettlementMethodsInDatabase)

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

    /**
     * Ensures that [OfferService.cancelOffer] and [BlockchainService.cancelOfferAsync] function properly.
     */
    @Test
    fun testCancelOffer() {

        val offerID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_cancelOffer"
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
            testingServerClient.get(testingServiceUrl){
                url {
                    parameters.append("events", "offer-opened")
                    parameters.append("offerID", offerID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.valueOf(10000L),
            amountUpperBound = BigInteger.valueOf(10000L),
            securityDepositAmount = BigInteger.valueOf(1000L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(),
            protocolVersion = BigInteger.ONE,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OFFER_OPENED
        )
        offerTruthSource.addOffer(offer)
        val offerIDByteBuffer = ByteBuffer.wrap(ByteArray(16))
        offerIDByteBuffer.putLong(offerID.mostSignificantBits)
        offerIDByteBuffer.putLong(offerID.leastSignificantBits)
        val offerIDByteArray = offerIDByteBuffer.array()
        val encoder = Base64.getEncoder()
        val offerIDString = encoder.encodeToString(offerIDByteArray)
        val offerForDatabase = DatabaseOffer(
            id = offerIDString,
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.direction.string,
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
        )

        runBlocking {
            databaseService.storeOffer(offerForDatabase)
            offerService.cancelOffer(
                offerID = offer.id,
                chainID = offer.chainID
            )
            assertEquals(OfferState.CANCEL_OFFER_TRANSACTION_BROADCAST, offerTruthSource.offers[offerID]?.state)

            val offerInDatabase = databaseService.getOffer(offerIDString)
            assertEquals("cancelOfferTxBroadcast", offerInDatabase!!.state)

            val offerStruct = blockchainService.getOffer(offerID)
            assertNull(offerStruct)
        }

    }

    /**
     * Ensures that [OfferService.editOffer] and [BlockchainService.editOfferAsync] function properly.
     */
    @Test
    fun testEditOffer() {

        val offerID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_editOffer"
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
            testingServerClient.get(testingServiceUrl){
                url {
                    parameters.append("events", "offer-opened")
                    parameters.append("offerID", offerID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        runBlocking {
            offerService.editOffer(
                offerID = offerID,
                newSettlementMethods = listOf(
                    SettlementMethod(
                        currency = "USD",
                        method = "a_method",
                        price = "1.23"
                    )
                )
            )

            val expectedSettlementMethods = listOf(
                Json.encodeToString(
                    SettlementMethod(
                        currency = "USD",
                        method = "a_method",
                        price = "1.23"
                    )
                ).encodeToByteArray()
            )

            val offerStruct = blockchainService.getOffer(offerID)
            assertEquals(expectedSettlementMethods.size, offerStruct!!.settlementMethods.size)
            offerStruct.settlementMethods.indices.forEach {
                assert(expectedSettlementMethods[it].contentEquals(offerStruct.settlementMethods[it]))
            }
        }
    }

    /**
     * Ensures that [OfferService.takeOffer], [BlockchainService.approveTokenTransferAsync] and
     * [BlockchainService.takeOfferAsync] function properly.
     */
    @Test
    fun testTakeOffer() {

        val offerID = UUID.randomUUID()

        val settlementMethodString = Json.encodeToString(SettlementMethod(
            currency = "EUR",
            price = "0.98",
            method = "SEPA",
        ))

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)

        val testingServiceUrl = "http://localhost:8546/test_offerservice_takeOffer"
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
            testingServerClient.get(testingServiceUrl){
                url {
                    parameters.append("events", "offer-opened")
                    parameters.append("offerID", offerID.toString())
                    parameters.append("settlement_method_string", settlementMethodString)
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )

        val offerTruthSource = TestOfferTruthSource()
        offerService.setOfferTruthSource(offerTruthSource)

        val swapTruthSource = TestSwapTruthSource()
        offerService.setSwapTruthSource(newTruthSource = swapTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266",
            interfaceId = ByteArray(0),
            stablecoin = testingServerResponse.stablecoinAddress,
            amountLowerBound = BigInteger.valueOf(10_000L) * BigInteger.TEN.pow(18),
            amountUpperBound = BigInteger.valueOf(20_000L) * BigInteger.TEN.pow(18),
            securityDepositAmount = BigInteger.valueOf(2_000L) * BigInteger.TEN.pow(18),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(
                SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                )
            ),
            protocolVersion = BigInteger.ONE,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )

        offerTruthSource.addOffer(offer)
        val encoder = Base64.getEncoder()
        val offerIDB64String = encoder.encodeToString(offerID.asByteArray())
        val offerForDatabase = DatabaseOffer(
            id = offerIDB64String,
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceId),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.direction.string,
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)

            val offerInDatabaseBeforeCancellation = databaseService.getOffer(id = offerIDB64String)
            assertEquals(offerForDatabase, offerInDatabaseBeforeCancellation)

            val swapData = ValidatedNewSwapData(
                takenSwapAmount = BigInteger.valueOf(15_000L) * BigInteger.TEN.pow(18),
                settlementMethod = SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                ),
            )

            offerService.takeOffer(
                offerToTake = offer,
                swapData = swapData,
            )

            val swapOnChain = blockchainService.getSwap(id = offerID)
            assertTrue(swapOnChain!!.isCreated)

            assertNotNull(keyManagerService.getKeyPair(interfaceId = swapOnChain.takerInterfaceID))

            val swapInTruthSource = swapTruthSource.swaps[offerID]
            assertTrue(swapInTruthSource!!.isCreated)
            assertFalse(swapInTruthSource.requiresFill)
            assertEquals(offerID, swapInTruthSource.id)
            assertEquals("0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", swapInTruthSource.maker)
            assert(ByteArray(0).contentEquals(swapInTruthSource.makerInterfaceID))
            // TODO: check taker address once walletService is implemented
            assert(swapOnChain.takerInterfaceID.contentEquals(swapInTruthSource.takerInterfaceID))
            assertEquals(testingServerResponse.stablecoinAddress, swapInTruthSource.stablecoin)
            assertEquals(BigInteger.valueOf(10_000L) * BigInteger.TEN.pow(18), swapInTruthSource
                .amountLowerBound)
            assertEquals(BigInteger.valueOf(20_000L) * BigInteger.TEN.pow(18), swapInTruthSource
                .amountUpperBound)
            assertEquals(BigInteger.valueOf(2_000L) * BigInteger.TEN.pow(18), swapInTruthSource
                .securityDepositAmount)
            assertEquals(BigInteger.valueOf(15_000L) * BigInteger.TEN.pow(18), swapInTruthSource
                .takenSwapAmount)
            assertEquals(BigInteger.valueOf(150L) * BigInteger.TEN.pow(18), swapInTruthSource
                .serviceFeeAmount)
            assertEquals(BigInteger.valueOf(100L), swapInTruthSource.serviceFeeRate)
            assertEquals(BigInteger.ZERO, swapInTruthSource.onChainDirection)
            assertEquals(OfferDirection.BUY, swapInTruthSource.direction)
            assert(Json.encodeToString(SettlementMethod(
                currency = "EUR",
                price = "0.98",
                method = "SEPA",
            )).encodeToByteArray().contentEquals(swapInTruthSource.onChainSettlementMethod))
            assertEquals(SettlementMethod(
                currency = "EUR",
                price = "0.98",
                method = "SEPA",
            ), swapInTruthSource.settlementMethod)
            assertEquals(BigInteger.ONE, swapInTruthSource.protocolVersion)
            assertFalse(swapInTruthSource.isPaymentSent)
            assertFalse(swapInTruthSource.isPaymentReceived)
            assertFalse(swapInTruthSource.hasBuyerClosed)
            assertFalse(swapInTruthSource.hasSellerClosed)
            assertEquals(BigInteger.ZERO, swapInTruthSource.onChainDisputeRaiser)
            assertEquals(BigInteger.valueOf(31337L), swapInTruthSource.chainID)
            assertEquals(SwapState.TAKE_OFFER_TRANSACTION_BROADCAST, swapInTruthSource.state)

            // Test the persistently stored swap
            val swapInDatabase = databaseService.getSwap(id = offerIDB64String)
            // TODO: check proper taker address once WalletService is implemented
            val expectedSwapInDatabase = DatabaseSwap(
                id = offerIDB64String,
                isCreated = 1L,
                requiresFill = 0L,
                maker = swapInTruthSource.maker,
                makerInterfaceID = "",
                taker = swapInTruthSource.taker,
                takerInterfaceID = "",
                stablecoin = swapInTruthSource.stablecoin,
                amountLowerBound = swapInTruthSource.amountLowerBound.toString(),
                amountUpperBound = swapInTruthSource.amountUpperBound.toString(),
                securityDepositAmount = swapInTruthSource.securityDepositAmount.toString(),
                takenSwapAmount = swapInTruthSource.takenSwapAmount.toString(),
                serviceFeeAmount = swapInTruthSource.serviceFeeAmount.toString(),
                serviceFeeRate = swapInTruthSource.serviceFeeRate.toString(),
                onChainDirection = swapInTruthSource.onChainDirection.toString(),
                settlementMethod = encoder.encodeToString(Json.encodeToString(SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                )).encodeToByteArray()),
                protocolVersion = swapInTruthSource.protocolVersion.toString(),
                isPaymentSent = 0L,
                isPaymentReceived = 0L,
                hasBuyerClosed = 0L,
                hasSellerClosed = 0L,
                disputeRaiser = swapInTruthSource.onChainDisputeRaiser.toString(),
                chainID = swapInTruthSource.chainID.toString(),
                state = swapInTruthSource.state.value.asString,
                role = swapInTruthSource.role.asString,
            )
            assertEquals(expectedSwapInDatabase.id, swapInDatabase!!.id)
            assertEquals(expectedSwapInDatabase.isCreated, swapInDatabase.isCreated)
            assertEquals(expectedSwapInDatabase.requiresFill, swapInDatabase.requiresFill)
            assertEquals(expectedSwapInDatabase.maker, swapInDatabase.maker)
            assertEquals(expectedSwapInDatabase.taker, swapInDatabase.taker)
            assertEquals(expectedSwapInDatabase.stablecoin, swapInDatabase.stablecoin)
            assertEquals(expectedSwapInDatabase.amountLowerBound, swapInDatabase.amountLowerBound)
            assertEquals(expectedSwapInDatabase.amountUpperBound, swapInDatabase.amountUpperBound)
            assertEquals(expectedSwapInDatabase.securityDepositAmount, swapInDatabase.securityDepositAmount)
            assertEquals(expectedSwapInDatabase.takenSwapAmount, swapInDatabase.takenSwapAmount)
            assertEquals(expectedSwapInDatabase.serviceFeeAmount, swapInDatabase.serviceFeeAmount)
            assertEquals(expectedSwapInDatabase.serviceFeeRate, swapInDatabase.serviceFeeRate)
            assertEquals(expectedSwapInDatabase.onChainDirection, swapInDatabase.onChainDirection)
            assertEquals(expectedSwapInDatabase.settlementMethod, swapInDatabase.settlementMethod)
            assertEquals(expectedSwapInDatabase.protocolVersion, swapInDatabase.protocolVersion)
            assertEquals(expectedSwapInDatabase.isPaymentSent, swapInDatabase.isPaymentSent)
            assertEquals(expectedSwapInDatabase.isPaymentReceived, swapInDatabase.isPaymentReceived)
            assertEquals(expectedSwapInDatabase.hasBuyerClosed, swapInDatabase.hasBuyerClosed)
            assertEquals(expectedSwapInDatabase.hasSellerClosed, swapInDatabase.hasSellerClosed)
            assertEquals(expectedSwapInDatabase.disputeRaiser, swapInDatabase.disputeRaiser)
            assertEquals(expectedSwapInDatabase.chainID, swapInDatabase.chainID)
            assertEquals(expectedSwapInDatabase.state, swapInDatabase.state)
            assertEquals(expectedSwapInDatabase.role, swapInDatabase.role)

            // Ensure the Offer is in the "taken" state
            assertEquals(OfferState.TAKEN, offer.state)

            // Ensure the value of the Offer's isTaken property is true
            assertTrue(offer.isTaken.value)

            // Check that the offer has been deleted from persistent storage
            assertNull(databaseService.getOffer(id = offerIDB64String))
        }

    }

}