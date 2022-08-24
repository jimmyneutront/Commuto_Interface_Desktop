package com.commuto.interfacedesktop.swap

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.commuto.interfacedesktop.blockchain.BlockchainExceptionNotifiable
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.OfferNotifiable
import com.commuto.interfacedesktop.offer.TestSwapTruthSource
import com.commuto.interfacedesktop.p2p.OfferMessageNotifiable
import com.commuto.interfacedesktop.p2p.P2PExceptionNotifiable
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
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

        // The ID of the swap for which taker information will be announced
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

        /*
        We must persistently store a swap with an ID equal to swapID and the maker and taker interface IDs from the keys
        created above, otherwise SwapService won't be able to get the keys necessary to make the taker info announcement
         */
        val encoder = Base64.getEncoder()
        val swapForDatabase = DatabaseSwap(
            id = encoder.encodeToString(swapID.asByteArray()),
            isCreated = 1L,
            requiresFill = 0L,
            maker = "",
            makerInterfaceID = encoder.encodeToString(makerKeyPair.interfaceId),
            taker = "",
            takerInterfaceID = encoder.encodeToString(takerKeyPair.interfaceId),
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
            chainID = "31337",
            state = "",
        )
        databaseService.storeSwap(swapForDatabase)

        // TODO: Move this to its own class
        class TestOfferService : OfferMessageNotifiable {
            override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {}
        }

        class TestP2PExceptionHandler : P2PExceptionNotifiable {
            @Throws
            override fun handleP2PException(exception: Exception) {
                throw exception
            }
        }
        val p2pExceptionHandler = TestP2PExceptionHandler()
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = System.getenv("MXKY") }
        class TestP2PService: P2PService(
            exceptionHandler = p2pExceptionHandler,
            offerService = TestOfferService(),
            mxClient = mxClient
        ) {
            var makerPublicKey: PublicKey? = null
            var takerKeyPair: KeyPair? = null
            var swapID: UUID? = null
            var settlementMethodDetails: String? = null
            override suspend fun sendTakerInformation(
                makerPublicKey: PublicKey,
                takerKeyPair: KeyPair,
                swapID: UUID,
                settlementMethodDetails: String
            ) {
                this.makerPublicKey = makerPublicKey
                this.takerKeyPair = takerKeyPair
                this.swapID = swapID
                this.settlementMethodDetails = settlementMethodDetails
            }
        }
        val p2pService = TestP2PService()
        swapService.setP2PService(p2pService)
        swapService.setSwapTruthSource(TestSwapTruthSource())

        swapService.sendTakerInformationMessage(
            swapID = swapID,
            chainID = BigInteger.valueOf(31337L)
        )

        assert(makerKeyPair.interfaceId.contentEquals(p2pService.makerPublicKey!!.interfaceId))
        assert(takerKeyPair.interfaceId.contentEquals(p2pService.takerKeyPair!!.interfaceId))
        assertEquals(swapID, p2pService.swapID!!)
        // TODO: update this once SettlementMethodService is added
        assertEquals("TEMPORARY", p2pService.settlementMethodDetails!!)

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

        class TestBlockchainExceptionHandler: BlockchainExceptionNotifiable {
            var gotError = false
            override fun handleBlockchainException(exception: Exception) {
                gotError = true
            }
        }
        val exceptionHandler = TestBlockchainExceptionHandler()

        class TestOfferService: OfferNotifiable {
            override suspend fun handleOfferOpenedEvent(event: OfferOpenedEvent) {}
            override suspend fun handleOfferEditedEvent(event: OfferEditedEvent) {}
            override suspend fun handleOfferCanceledEvent(event: OfferCanceledEvent) {}
            override suspend fun handleOfferTakenEvent(event: OfferTakenEvent) {}
            override suspend fun handleServiceFeeRateChangedEvent(event: ServiceFeeRateChangedEvent) {}
        }

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        swapService.setBlockchainService(blockchainService)
        swapService.handleNewSwap(swapID = swapID, chainID = BigInteger.valueOf(31337))

        assertEquals(swapID, swapTruthSource.addedSwap!!.id)

    }

}