package com.commuto.interfacedesktop.swap

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.TestBlockchainExceptionHandler
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OfferState
import com.commuto.interfacedesktop.offer.TestOfferService
import com.commuto.interfacedesktop.p2p.*
import com.commuto.interfacedesktop.p2p.messages.MakerInformationMessage
import com.commuto.interfacedesktop.p2p.messages.TakerInformationMessage
import com.commuto.interfacedesktop.settlement.SettlementMethod
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*

/**
 * Tests for [SwapService]
 */
class SwapServiceTests {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * Ensures that [SwapService] sends taker information correctly.
     */
    @Test
    fun testSendTakerInformation() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // Set up SwapService
        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)

        // The ID of the swap for which taker information will be sent
        val swapID = UUID.randomUUID()

        /*
        The public key of this key pair will be the maker's public key (NOT the user's/taker's), so we do NOT want to
        store the whole key pair persistently
         */
        val makerKeyPair = keyManagerService.generateKeyPair(storeResult = false)
        // We store the maker's public key, as if we received it via the P2P network
        keyManagerService.storePublicKey(makerKeyPair.getPublicKey())
        // This is the taker's (user's) key pair, so we do want to store it persistently
        val takerKeyPair = keyManagerService.generateKeyPair(storeResult = true)

        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = makerKeyPair.interfaceId,
            taker = "",
            takerInterfaceID = takerKeyPair.interfaceId,
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                 {
                     "f": "USD",
                     "p": "1.00",
                     "m": "SWIFT"
                 }
                 """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_TAKER_INFORMATION,
            role = SwapRole.TAKER_AND_SELLER,
        )
        swap.takerPrivateSettlementMethodData = "some_SWIFT_data"
        swapTruthSource.swaps[swapID] = swap

        /*
        We must persistently store a swap with an ID equal to swapID and the maker and taker interface IDs from the keys
        created above, otherwise SwapService won't be able to get the keys necessary to make the taker info message
         */
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        val p2pExceptionHandler = TestP2PExceptionHandler()
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }
        class TestP2PService: P2PService(
            exceptionHandler = p2pExceptionHandler,
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var makerPublicKey: PublicKey? = null
            var takerKeyPair: KeyPair? = null
            var swapID: UUID? = null
            var settlementMethodDetails: String? = null
            override suspend fun sendTakerInformation(
                makerPublicKey: PublicKey,
                takerKeyPair: KeyPair,
                swapID: UUID,
                settlementMethodDetails: String?
            ) {
                this.makerPublicKey = makerPublicKey
                this.takerKeyPair = takerKeyPair
                this.swapID = swapID
                this.settlementMethodDetails = settlementMethodDetails
            }
        }
        val p2pService = TestP2PService()
        swapService.setP2PService(p2pService)

        val didSendTakerInfoMessage = swapService.sendTakerInformationMessage(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337L)
        )
        assert(didSendTakerInfoMessage)

        assert(makerKeyPair.interfaceId.contentEquals(p2pService.makerPublicKey!!.interfaceId))
        assert(takerKeyPair.interfaceId.contentEquals(p2pService.takerKeyPair!!.interfaceId))
        assertEquals(swapID, p2pService.swapID!!)
        assertEquals("some_SWIFT_data", p2pService.settlementMethodDetails!!)

        assertEquals(SwapState.AWAITING_MAKER_INFORMATION, swap.state.value)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_MAKER_INFORMATION.asString, swapInDatabase!!.state)

    }

    /**
     * Ensures that [SwapService] handles new swaps made by the user of the interface properly.
     */
    @Test
    fun testHandleNewSwap() = runBlocking {
        val swapID = UUID.randomUUID()
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_swapservice_handleNewSwap"
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
                    parameters.append("offerID", swapID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        class TestSwapTruthSource: SwapTruthSource {
            var addedSwap: Swap? = null
            override var swaps: SnapshotStateMap<UUID, Swap> = mutableStateMapOf()
            override fun addSwap(swap: Swap) {
                swaps[swap.id] = swap
                addedSwap = swap
            }
        }
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(blockchainService)

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = swapID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.valueOf(10000L),
            amountUpperBound = BigInteger.valueOf(10000L),
            securityDepositAmount = BigInteger.valueOf(1000L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(
                SettlementMethod(currency = "USD", price = "1.00", method = "SWIFT", privateData = "SWIFT_private_data")
            ),
            protocolVersion = BigInteger.valueOf(1),
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OFFER_OPENED
        )

        swapService.handleNewSwap(takenOffer = offer)

        assertEquals(swapID, swapTruthSource.addedSwap!!.id)

    }

    /**
     * Ensure that [SwapService] handles [TakerInformationMessage]s properly.
     */
    @Test
    fun testHandleTakerInformationMessage() = runBlocking {
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap for which taker information is being received
        val swapID = UUID.randomUUID()
        // The taker's key pair. Since we are the maker, this is NOT stored persistently.
        val takerKeyPair = KeyPair()
        // The maker's key pair. Since we are the maker, this is stored persistently
        val makerKeyPair = keyManagerService.generateKeyPair(storeResult = true)

        // The taker's information that the maker must handle
        val message = TakerInformationMessage(
            swapID = swapID,
            publicKey = takerKeyPair.getPublicKey(),
            settlementMethodDetails = "taker_settlement_method_details"
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }

        // A test P2PService that will allow us to get the maker information that will be sent
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = swapService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var takerPublicKey: PublicKey? = null
            var makerKeyPair: KeyPair? = null
            var swapID: UUID? = null
            var settlementMethodDetails: String? = null
            override suspend fun sendMakerInformation(
                takerPublicKey: PublicKey,
                makerKeyPair: KeyPair,
                swapID: UUID,
                settlementMethodDetails: String?
            ) {
                this.takerPublicKey = takerPublicKey
                this.makerKeyPair = makerKeyPair
                this.swapID = swapID
                this.settlementMethodDetails = settlementMethodDetails
            }
        }
        val p2pService = TestP2PService()

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        // Provide swapTruthSource to swapService
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = makerKeyPair.interfaceId,
            taker = "",
            takerInterfaceID = takerKeyPair.interfaceId,
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                 {
                     "f": "USD",
                     "p": "1.00",
                     "m": "SWIFT"
                 }
                 """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_TAKER_INFORMATION,
            role = SwapRole.MAKER_AND_SELLER,
        )
        swap.makerPrivateSettlementMethodData = "maker_settlement_method_details"
        swapTruthSource.addSwap(swap)
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = swap.makerPrivateSettlementMethodData,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handleTakerInformationMessage(message)

        assert(takerKeyPair.interfaceId.contentEquals(p2pService.takerPublicKey!!.interfaceId))
        assert(makerKeyPair.interfaceId.contentEquals(p2pService.makerKeyPair!!.interfaceId))
        assertEquals(swapID, p2pService.swapID!!)
        assertEquals("maker_settlement_method_details", p2pService.settlementMethodDetails)

        // Ensure that SwapService updates swap state and taker private data in swapTruthSource
        assertEquals(SwapState.AWAITING_FILLING, swap.state.value)
        assertEquals("taker_settlement_method_details", swap.takerPrivateSettlementMethodData)

        // Ensure that SwapService persistently updates swap state and taker private data
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_FILLING.asString, swapInDatabase!!.state)
        assertEquals("taker_settlement_method_details", swapInDatabase.takerPrivateData)
    }

    /**
     * Ensure that [SwapService] handles [MakerInformationMessage]s properly.
     */
    @Test
    fun testHandleMakerInformationMessage() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap for which maker information is being received
        val swapID = UUID.randomUUID()
        // The maker's key pair. Since we are the taker, this is NOT stored persistently.
        val makerKeyPair = KeyPair()
        keyManagerService.storePublicKey(makerKeyPair.getPublicKey())
        // The taker's key pair. Since we are the taker, this is stored persistently
        val takerKeyPair = keyManagerService.generateKeyPair(storeResult = true)

        // The maker's information that the taker must handle
        val message = MakerInformationMessage(
            swapID = swapID,
            settlementMethodDetails = "maker_settlement_method_details",
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        // Provide swapTruthSource to swapService
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = makerKeyPair.interfaceId,
            taker = "",
            takerInterfaceID = takerKeyPair.interfaceId,
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                 {
                     "f": "USD",
                     "p": "1.00",
                     "m": "SWIFT"
                 }
                 """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_MAKER_INFORMATION,
            role = SwapRole.TAKER_AND_BUYER
        )
        swapTruthSource.addSwap(swap)
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handleMakerInformationMessage(
            message = message,
            senderInterfaceID = makerKeyPair.interfaceId,
            recipientInterfaceID = takerKeyPair.interfaceId,
        )

        assertEquals(SwapState.AWAITING_FILLING, swap.state.value)
        assertEquals("maker_settlement_method_details", swap.makerPrivateSettlementMethodData)

        // Ensure that SwapService persistently updates swap state
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_FILLING.asString, swapInDatabase!!.state)
        assertEquals("maker_settlement_method_details", swapInDatabase.makerPrivateData)
    }

    /**
     * Ensures that [SwapService.fillSwap] and [BlockchainService.fillSwapAsync] function properly.
     */
    @Test
    fun testFillSwap() = runBlocking {
        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_swapservice_fillSwap"
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
                    parameters.append("events", "offer-opened-taken")
                    parameters.append("swapID", swapID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(newBlockchainService = blockchainService)

        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = testingServerResponse.stablecoinAddress,
            amountLowerBound = BigInteger.valueOf(10_000L),
            amountUpperBound = BigInteger.valueOf(10_000L),
            securityDepositAmount = BigInteger.valueOf(1_000L),
            takenSwapAmount = BigInteger.valueOf(10_000L),
            serviceFeeAmount = BigInteger.valueOf(100L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                 {
                     "f": "USD",
                     "p": "1.00",
                     "m": "SWIFT"
                 }
                 """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_FILLING,
            role = SwapRole.MAKER_AND_SELLER,
        )
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.fillSwap(swapToFill = swap)

        assertEquals(SwapState.FILL_SWAP_TRANSACTION_BROADCAST, swap.state.value)
        assertFalse(swap.requiresFill)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(0L, swapInDatabase!!.requiresFill)
        assertEquals(SwapState.FILL_SWAP_TRANSACTION_BROADCAST.asString, swapInDatabase.state)

    }

    /**
     * Ensure that [SwapService] handles [SwapFilledEvent]s properly.
     */
    @Test
    fun testHandleSwapFilledEvent() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap being filled
        val swapID = UUID.randomUUID()

        val event = SwapFilledEvent(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337)
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                  {
                      "f": "USD",
                      "p": "1.00",
                      "m": "SWIFT"
                  }
                  """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_FILLING,
            role = SwapRole.MAKER_AND_SELLER,
        )
        swapTruthSource.swaps[swapID] = swap
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handleSwapFilledEvent(event = event)

        assertEquals(SwapState.AWAITING_PAYMENT_SENT, swap.state.value)
        assertFalse(swap.requiresFill)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_PAYMENT_SENT.asString, swapInDatabase!!.state)
        assertEquals(0L, swapInDatabase.requiresFill)

    }

    /**
     * Ensures that [SwapService.reportPaymentSent] and `[BlockchainService.reportPaymentSentAsync] function properly.
     */
    @Test
    fun testReportPaymentSent() = runBlocking {

        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_swapservice_reportPaymentSent"
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
                    parameters.append("events", "offer-opened-taken")
                    parameters.append("swapID", swapID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(newBlockchainService = blockchainService)

        val swap = Swap(
            isCreated = true,
            requiresFill = true,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = testingServerResponse.stablecoinAddress,
            amountLowerBound = BigInteger.valueOf(10_000L),
            amountUpperBound = BigInteger.valueOf(10_000L),
            securityDepositAmount = BigInteger.valueOf(1_000L),
            takenSwapAmount = BigInteger.valueOf(10_000L),
            serviceFeeAmount = BigInteger.valueOf(100L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            onChainSettlementMethod =
            """
                   {
                       "f": "USD",
                       "p": "1.00",
                       "m": "SWIFT"
                   }
                   """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_PAYMENT_SENT,
            role = SwapRole.MAKER_AND_BUYER,
        )
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.reportPaymentSent(
            swap = swap,
        )

        assertEquals(SwapState.REPORT_PAYMENT_SENT_TRANSACTION_BROADCAST, swap.state.value)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.REPORT_PAYMENT_SENT_TRANSACTION_BROADCAST.asString, swapInDatabase!!.state)

    }

    /**
     * Ensure that [SwapService] handles [PaymentSentEvent]s properly.
     */
    @Test
    fun testHandlePaymentSentEvent() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap for which payment is being sent
        val swapID = UUID.randomUUID()

        // The PaymentSentEvent to be handled
        val event = PaymentSentEvent(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337),
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            onChainSettlementMethod =
            """
                   {
                       "f": "USD",
                       "p": "1.00",
                       "m": "SWIFT"
                   }
                   """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_FILLING,
            role = SwapRole.MAKER_AND_BUYER,
        )
        swapTruthSource.swaps[swapID] = swap
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handlePaymentSentEvent(event)

        // Ensure that SwapService updates swap state and isPaymentSent in swapTruthSource
        assertEquals(SwapState.AWAITING_PAYMENT_RECEIVED, swap.state.value)
        assert(swap.isPaymentSent)

        // Ensure that SwapService updates swap state and isPaymentSent in persistent storage
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_PAYMENT_RECEIVED.asString, swapInDatabase!!.state)
        assertEquals(1L, swapInDatabase.isPaymentSent)

    }

    /**
     * Ensures that [SwapService.reportPaymentReceived] and `[BlockchainService.reportPaymentReceivedAsync] function
     * properly.
     */
    @Test
    fun testReportPaymentReceived() = runBlocking {

        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_swapservice_reportPaymentReceived"
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
                    parameters.append("events", "offer-opened-taken-PaymentSent")
                    parameters.append("swapID", swapID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(newBlockchainService = blockchainService)

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = testingServerResponse.stablecoinAddress,
            amountLowerBound = BigInteger.valueOf(10_000L),
            amountUpperBound = BigInteger.valueOf(10_000L),
            securityDepositAmount = BigInteger.valueOf(1_000L),
            takenSwapAmount = BigInteger.valueOf(10_000L),
            serviceFeeAmount = BigInteger.valueOf(100L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            onChainSettlementMethod =
            """
                   {
                       "f": "USD",
                       "p": "1.00",
                       "m": "SWIFT"
                   }
                   """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_PAYMENT_RECEIVED,
            role = SwapRole.TAKER_AND_SELLER,
        )
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.reportPaymentReceived(
            swap = swap,
        )

        assertEquals(SwapState.REPORT_PAYMENT_RECEIVED_TRANSACTION_BROADCAST, swap.state.value)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.REPORT_PAYMENT_RECEIVED_TRANSACTION_BROADCAST.asString, swapInDatabase!!.state)

    }

    /**
     * Ensure that [SwapService] handles [PaymentReceivedEvent]s properly.
     */
    @Test
    fun testHandlePaymentReceivedEvent() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap for which payment is being sent
        val swapID = UUID.randomUUID()

        // The PaymentReceivedEvent to be handled
        val event = PaymentReceivedEvent(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337),
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
                   {
                       "f": "USD",
                       "p": "1.00",
                       "m": "SWIFT"
                   }
                   """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.REPORT_PAYMENT_RECEIVED_TRANSACTION_BROADCAST,
            role = SwapRole.MAKER_AND_SELLER,
        )
        swapTruthSource.swaps[swapID] = swap
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 1L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handlePaymentReceivedEvent(event)

        // Ensure that SwapService updates swap state and isPaymentReceived in swapTruthSource
        assertEquals(SwapState.AWAITING_CLOSING, swap.state.value)
        assert(swap.isPaymentReceived)

        // Ensure that SwapService updates swap state and isPaymentReceived in persistent storage
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.AWAITING_CLOSING.asString, swapInDatabase!!.state)
        assertEquals(1L, swapInDatabase.isPaymentReceived)

    }

    /**
     * Ensures that [SwapService.closeSwap] and `[BlockchainService.closeSwapAsync] function properly.
     */
    @Test
    fun testCloseSwap() = runBlocking {

        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_swapservice_closeSwap"
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
                    parameters.append("events", "offer-opened-taken-PaymentSent-Received")
                    parameters.append("swapID", swapID.toString())
                }
            }.body()
        }

        val w3 = Web3j.build(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(newBlockchainService = blockchainService)

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = testingServerResponse.stablecoinAddress,
            amountLowerBound = BigInteger.valueOf(10_000L),
            amountUpperBound = BigInteger.valueOf(10_000L),
            securityDepositAmount = BigInteger.valueOf(1_000L),
            takenSwapAmount = BigInteger.valueOf(10_000L),
            serviceFeeAmount = BigInteger.valueOf(100L),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            onChainSettlementMethod =
            """
                   {
                       "f": "USD",
                       "p": "1.00",
                       "m": "SWIFT"
                   }
                   """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.AWAITING_CLOSING,
            role = SwapRole.MAKER_AND_BUYER,
        )
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 1L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.closeSwap(
            swap = swap,
        )

        assertEquals(SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST, swap.state.value)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST.asString, swapInDatabase!!.state)

    }

    /**
     * Ensure that [SwapService] handles [BuyerClosedEvent]s properly.
     */
    @Test
    fun testHandleBuyerClosedEvent() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap that the buyer has closed
        val swapID = UUID.randomUUID()

        // The BuyerClosedEvent to be handled
        val event = BuyerClosedEvent(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337),
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            onChainSettlementMethod =
            """
            {
                "f": "USD",
                "p": "1.00",
                "m": "SWIFT"
            }
            """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = false,
            isPaymentReceived = false,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST,
            role = SwapRole.MAKER_AND_BUYER,
        )
        swapTruthSource.swaps[swapID] = swap
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 1L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handleBuyerClosedEvent(
            event = event,
        )

        // Ensure that SwapService updates swap state and hasBuyerClosed in swapTruthSource
        assertEquals(SwapState.CLOSED, swap.state.value)
        assert(swap.hasBuyerClosed)

        // Ensure that SwapService updates swap state and hasBuyerClosed in persistent storage
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.CLOSED.asString, swapInDatabase!!.state)
        assertEquals(1L, swapInDatabase.hasBuyerClosed)

    }

    /**
     * Ensure that [SwapService] handles [SellerClosedEvent]s properly.
     */
    @Test
    fun testHandleSellerClosedEvent() = runBlocking {
        // Set up DatabaseService and KeyManagerService
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        // The ID of the swap that the seller has closed
        val swapID = UUID.randomUUID()

        // The SellerClosedEvent to be handled
        val event = SellerClosedEvent(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337),
        )

        val swapService = SwapService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )

        // Create swap, add it to swapTruthSource, and save it persistently
        val swapTruthSource = TestSwapTruthSource()
        swapService.setSwapTruthSource(swapTruthSource)
        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = ByteArray(0),
            stablecoin = "",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            takenSwapAmount = BigInteger.ZERO,
            serviceFeeAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.SELL,
            onChainSettlementMethod =
            """
            {
                "f": "USD",
                "p": "1.00",
                "m": "SWIFT"
            }
            """.trimIndent().encodeToByteArray(),
            protocolVersion = BigInteger.ZERO,
            isPaymentSent = true,
            isPaymentReceived = true,
            hasBuyerClosed = false,
            hasSellerClosed = false,
            onChainDisputeRaiser = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            state = SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST,
            role = SwapRole.MAKER_AND_SELLER,
        )
        swapTruthSource.swaps[swapID] = swap
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = swap.maker,
            makerInterfaceID = encoder.encodeToString(swap.makerInterfaceID),
            taker = swap.taker,
            takerInterfaceID = encoder.encodeToString(swap.takerInterfaceID),
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound.toString(),
            amountUpperBound = swap.amountUpperBound.toString(),
            securityDepositAmount = swap.securityDepositAmount.toString(),
            takenSwapAmount = swap.takenSwapAmount.toString(),
            serviceFeeAmount = swap.serviceFeeAmount.toString(),
            serviceFeeRate = swap.serviceFeeRate.toString(),
            onChainDirection = swap.onChainDirection.toString(),
            settlementMethod = encoder.encodeToString(swap.onChainSettlementMethod),
            makerPrivateData = null,
            makerPrivateDataInitializationVector = null,
            takerPrivateData = null,
            takerPrivateDataInitializationVector = null,
            protocolVersion = swap.protocolVersion.toString(),
            isPaymentSent = 1L,
            isPaymentReceived = 1L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = swap.onChainDisputeRaiser.toString(),
            chainID = swap.chainID.toString(),
            state = swap.state.value.asString,
            role = swap.role.asString
        )
        databaseService.storeSwap(swapForDatabase)

        swapService.handleSellerClosedEvent(
            event = event,
        )

        // Ensure that SwapService updates swap state and hasSellerClosed in swapTruthSource
        assertEquals(SwapState.CLOSED, swap.state.value)
        assert(swap.hasSellerClosed)

        // Ensure that SwapService updates swap state and hasSellerClosed in persistent storage
        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(SwapState.CLOSED.asString, swapInDatabase!!.state)
        assertEquals(1L, swapInDatabase.hasSellerClosed)

    }

}