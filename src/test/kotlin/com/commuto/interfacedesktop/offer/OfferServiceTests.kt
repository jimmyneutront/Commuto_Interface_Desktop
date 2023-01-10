package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.CommutoWeb3j
import com.commuto.interfacedesktop.blockchain.*
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.events.erc20.ApprovalEvent
import com.commuto.interfacedesktop.blockchain.events.erc20.TokenTransferApprovalPurpose
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.offer.validation.ValidatedNewSwapData
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.TestP2PExceptionHandler
import com.commuto.interfacedesktop.p2p.TestSwapMessageNotifiable
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import com.commuto.interfacedesktop.swap.SwapNotifiable
import com.commuto.interfacedesktop.swap.SwapState
import com.commuto.interfacedesktop.swap.TestSwapService
import com.commuto.interfacedesktop.swap.TestSwapTruthSource
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.offer.PreviewableOfferTruthSource
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*
import com.commuto.interfacedesktop.db.Offer as DatabaseOffer
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap

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
     * Ensure [OfferService] properly handles failed transactions that approve token transfers in order to open a new
     * offer.
     */
    @Test
    fun testHandleFailedApproveToOpenTransaction() = runBlocking {
        val offerID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerTruthSource = TestOfferTruthSource()

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.APPROVING_TRANSFER
        )
        offer.approvingToOpenState.value = TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION
        val approvingToOpenTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER
        )
        offer.approvingToOpenTransaction = approvingToOpenTransaction
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )
        offerService.setOfferTruthSource(offerTruthSource)

        offerService.handleFailedTransaction(
            transaction = approvingToOpenTransaction,
            exception = BlockchainTransactionException(message = "tx failed")
        )

        assertEquals(OfferState.TRANSFER_APPROVAL_FAILED, offer.state)
        assertEquals(TokenTransferApprovalState.EXCEPTION, offer.approvingToOpenState.value)
        assertNotNull(offer.approvingToOpenException)
        val offerInDatabase = databaseService.getOffer(id = encoder.encodeToString(offerID.asByteArray()))
        assertEquals(OfferState.TRANSFER_APPROVAL_FAILED.asString, offerInDatabase?.state)
        assertEquals(TokenTransferApprovalState.EXCEPTION.asString, offerInDatabase?.approveToOpenState)
    }

    /**
     * Ensure [OfferService] properly handles failed offer opening transactions.
     */
    @Test
    fun testHandleFailedOfferOpeningTransaction() = runBlocking {
        val offerID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerTruthSource = TestOfferTruthSource()

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.OPEN_OFFER_TRANSACTION_SENT
        )
        offer.openingOfferState.value = OpeningOfferState.AWAITING_TRANSACTION_CONFIRMATION
        val offerOpeningTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.OPEN_OFFER
        )
        offer.offerOpeningTransaction = offerOpeningTransaction
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )
        offerService.setOfferTruthSource(offerTruthSource)

        offerService.handleFailedTransaction(
            transaction = offerOpeningTransaction,
            exception = BlockchainTransactionException(message = "tx failed")
        )

        assertEquals(OfferState.AWAITING_OPENING, offer.state)
        assertEquals(OpeningOfferState.EXCEPTION, offer.openingOfferState.value)
        assertNotNull(offer.openingOfferException)
        val offerInDatabase = databaseService.getOffer(id = encoder.encodeToString(offerID.asByteArray()))
        assertEquals(OfferState.AWAITING_OPENING.asString, offerInDatabase?.state)
        assertEquals(OpeningOfferState.EXCEPTION.asString, offerInDatabase?.openingOfferState)
    }

    /**
     * Ensure [OfferService] handles failed offer cancellation transactions properly.
     */
    @Test
    fun testHandleFailedOfferCancellationTransaction() = runBlocking {
        val offerID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerTruthSource = TestOfferTruthSource()

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.OFFER_OPENED
        )
        offer.cancelingOfferState.value = CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION
        val offerCancellationTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.CANCEL_OFFER
        )
        offer.offerCancellationTransaction = offerCancellationTransaction
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )
        offerService.setOfferTruthSource(offerTruthSource)

        offerService.handleFailedTransaction(
            transaction = offerCancellationTransaction,
            exception = BlockchainTransactionException(message = "tx failed")
        )

        val offerInTruthSource = offerTruthSource.offers[offerID]
        assertEquals(CancelingOfferState.EXCEPTION, offerInTruthSource!!.cancelingOfferState.value)
        assertNotNull(offerInTruthSource.cancelingOfferException)
        val offerInDatabase = databaseService.getOffer(id = encoder.encodeToString(offerID.asByteArray()))
        assertEquals(CancelingOfferState.EXCEPTION.asString, offerInDatabase!!.cancelingOfferState)
    }

    /**
     * Ensure [OfferService] handles failed offer editing transactions properly.
     */
    @Test
    fun testHandleFailedOfferEditingTransaction() = runBlocking {
        val offerID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerTruthSource = TestOfferTruthSource()

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.OFFER_OPENED
        )
        val offerEditingTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.EDIT_OFFER
        )
        offer.offerEditingTransaction = offerEditingTransaction
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val pendingSettlementMethods = listOf(
            SettlementMethod(
                currency = "EUR",
                price = "0.98",
                method = "SEPA",
                privateData = "some_sepa_data"
            ),
            SettlementMethod(
                currency = "BSD",
                price = "1.00",
                method = "SANDDOLLAR",
                privateData = "some_sanddollar_data"
            ),
        )
        offer.selectedSettlementMethods.addAll(pendingSettlementMethods)
        val serializedPendingSettlementMethodsAndPrivateDetails = pendingSettlementMethods.map {
            Pair(Json.encodeToString(it), it.privateData)
        }
        databaseService.storePendingOfferSettlementMethods(
            offerID = encoder.encodeToString(offerID.asByteArray()),
            chainID = offerForDatabase.chainID,
            pendingSettlementMethods = serializedPendingSettlementMethodsAndPrivateDetails
        )

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )
        offerService.setOfferTruthSource(offerTruthSource)

        offerService.handleFailedTransaction(
            transaction = offerEditingTransaction,
            exception = BlockchainTransactionException(message = "tx failed")
        )

        assertEquals(EditingOfferState.EXCEPTION, offer.editingOfferState.value)
        assertNotNull(offer.editingOfferException)
        assertEquals(0, offer.selectedSettlementMethods.count())
        val offerInDatabase = databaseService.getOffer(id = encoder.encodeToString(offerID.asByteArray()))
        assertEquals(EditingOfferState.EXCEPTION.asString, offerInDatabase!!.editingOfferState)

        val pendingSettlementMethodsInDatabase = databaseService.getPendingOfferSettlementMethods(
            offerID = encoder.encodeToString(offerID.asByteArray()),
            chainID = offerForDatabase.chainID
        )
        assertNull(pendingSettlementMethodsInDatabase)

    }

    /**
     * Ensures that [OfferService] handles
     * [Approval](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-Approval-address-address-uint256-)
     * events properly, when such events indicate the approval of a token transfer in order to open an offer made by the
     * interface user.
     */
    @Test
    fun testHandleApprovalToOpenOfferEvent() = runBlocking {

        val offerID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val offerTruthSource = TestOfferTruthSource()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )
        offerService.setOfferTruthSource(offerTruthSource)

        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = offerID,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.APPROVE_TRANSFER_TRANSACTION_SENT
        )
        offer.approvingToOpenState.value = TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION
        val approvingToOpenTransaction = BlockchainTransaction(
            transactionHash = "0xa_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER
        )
        offer.approvingToOpenTransaction = approvingToOpenTransaction
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val event = ApprovalEvent(
            owner = "0x0000000000000000000000000000000000000000",
            spender = "0x0000000000000000000000000000000000000000",
            amount = BigInteger.ZERO,
            purpose = TokenTransferApprovalPurpose.OPEN_OFFER,
            transactionHash = "0xa_transaction_hash_here",
            chainID = offer.chainID
        )

        offerService.handleTokenTransferApprovalEvent(event = event)

        assertEquals(OfferState.AWAITING_OPENING, offer.state)
        assertEquals(TokenTransferApprovalState.COMPLETED, offer.approvingToOpenState.value)
        assertNull(offer.approvingToOpenException)
        val offerInDatabase = databaseService.getOffer(id = encoder.encodeToString(offerID.asByteArray()))
        assertEquals(OfferState.AWAITING_OPENING.asString, offerInDatabase?.state)
        assertEquals(TokenTransferApprovalState.COMPLETED.asString, offerInDatabase?.approveToOpenState)

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
        val expectedOfferIdByteArray = expectedOfferId.asByteArray()

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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
                val settlementMethodsInDatabase = databaseService.getOfferSettlementMethods(encoder
                    .encodeToString(expectedOfferIdByteArray), offerInDatabase.chainID)
                assertEquals(settlementMethodsInDatabase!!.size, 1)
                val decoder = Base64.getDecoder()
                val settlementMethodInDatabase = Json.decodeFromString<SettlementMethod>(
                    string = decoder.decode(settlementMethodsInDatabase[0].first).decodeToString()
                )
                assertEquals("USD", settlementMethodInDatabase.currency)
                assertEquals("SWIFT", settlementMethodInDatabase.price)
                assertEquals("1.00", settlementMethodInDatabase.method)
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

        val keyPair = keyManagerService.generateKeyPair(storeResult = true)

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
                }
            }.body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val offerTruthSource = TestOfferTruthSource()

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
        )

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
            interfaceID = keyPair.interfaceId,
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
            state = OfferState.OPEN_OFFER_TRANSACTION_SENT
        )
        offerTruthSource.addOffer(offer)
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(newOfferID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        databaseService.storeOffer(offerForDatabase)

        val exceptionHandler = TestBlockchainExceptionHandler()

        BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        val offerOpenedEvent = OfferOpenedEvent(
            offerID = newOfferID,
            interfaceID = keyPair.interfaceId,
            chainID = offer.chainID
        )

        offerService.handleOfferOpenedEvent(event = offerOpenedEvent)

        assertFalse(exceptionHandler.gotError)
        assertEquals(p2pService.offerIDForAnnouncement, newOfferID)
        assert(p2pService.keyPairForAnnouncement!!.interfaceId.contentEquals(keyPair.interfaceId))

        assertEquals(OfferState.OFFER_OPENED, offer.state)
        assertEquals(OpeningOfferState.COMPLETED, offer.openingOfferState.value)
        assertNull(offer.openingOfferException)
        val offerInDatabase = databaseService.getOffer(encoder.encodeToString(newOfferID.asByteArray()))
        assertEquals(OfferState.OFFER_OPENED.asString, offerInDatabase?.state)
        assertEquals(OpeningOfferState.COMPLETED.asString, offerInDatabase?.openingOfferState)

    }

    /**
     * Ensures that [OfferService] handles
     * [OfferCanceled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offercanceled) events properly,
     * both for offers made by the user of this interface and for offers that are not.
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        class TestBlockchainEventRepository: BlockchainEventRepository<OfferCanceledEvent>() {

            val appendedEventChannel = Channel<OfferCanceledEvent>()
            val removedEventChannel = Channel<OfferCanceledEvent>()

            override fun append(element: OfferCanceledEvent) {
                super.append(element)
                runBlocking {
                    appendedEventChannel.send(element)
                }
            }

            override fun remove(elementToRemove: OfferCanceledEvent) {
                super.remove(elementToRemove)
                runBlocking {
                    removedEventChannel.send(elementToRemove)
                }
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
                val addedOffer = offerTruthSource.offersChannel.receive()
                assertNotNull(offerTruthSource.offers[addedOffer.id])
            }
        }

        val openedOffer = offerTruthSource.offers[expectedOfferId]

        @Serializable
        data class SecondTestingServerResponse(val offerCancellationTransactionHash: String)

        val secondTestingServerResponse: SecondTestingServerResponse = runBlocking {
            testingServerClient.get(testingServiceUrl) {
                url {
                    parameters.append("events", "offer-canceled")
                    parameters.append("commutoSwapAddress", testingServerResponse.commutoSwapAddress)
                }
            }.body()
        }

        runBlocking {
            withTimeout(20_000) {
                val encoder = Base64.getEncoder()
                val appendedEvent = offerCanceledEventRepository.appendedEventChannel.receive()
                /*
                Wait for offer to be removed (since TestOfferTruthSource sends an offer to this channel upon removal)
                 */
                offerTruthSource.offersChannel.receive()
                val removedEvent = offerCanceledEventRepository.removedEventChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assert(offerTruthSource.offers.isEmpty())
                assertFalse(openedOffer!!.isCreated.value)
                assertEquals(OfferState.CANCELED, openedOffer.state)
                assertEquals(expectedOfferId, appendedEvent.offerID)
                assertEquals(expectedOfferId, removedEvent.offerID)
                val offerInDatabase = databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray))
                assertEquals(offerInDatabase, null)
            }
        }

        /*
        Ensure handleOfferCanceledEvent properly handles the cancellation of offers made by the user of this interface
         */
        blockchainService.stopListening()
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = expectedOfferId,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
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
            state = OfferState.OFFER_OPENED
        )
        offer.cancelingOfferState.value = CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION
        offer.offerCancellationTransaction = BlockchainTransaction(
            transactionHash = secondTestingServerResponse.offerCancellationTransactionHash,
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.CANCEL_OFFER
        )
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(expectedOfferId.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)
        }

        val offerCanceledEventRepositoryForMonitoredTxn = TestBlockchainEventRepository()
        val offerServiceForMonitoredTxn = OfferService(
            databaseService,
            keyManagerService,
            TestSwapService(),
            BlockchainEventRepository(),
            BlockchainEventRepository(),
            offerCanceledEventRepositoryForMonitoredTxn,
            BlockchainEventRepository(),
            BlockchainEventRepository(),
        )
        val offerTruthSourceForMonitoredTxn = TestOfferTruthSource()
        offerServiceForMonitoredTxn.setOfferTruthSource(offerTruthSourceForMonitoredTxn)
        offerTruthSourceForMonitoredTxn.offers[expectedOfferId] = offer
        val blockchainServiceForMonitoredTxn = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerServiceForMonitoredTxn,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainServiceForMonitoredTxn.addTransactionToMonitor(transaction = offer.offerCancellationTransaction!!)
        blockchainServiceForMonitoredTxn.listen()

        runBlocking {
            withTimeout(20_000) {
                val appendedEvent = offerCanceledEventRepositoryForMonitoredTxn.appendedEventChannel.receive()
                /*
                Wait for offer to be removed (since TestOfferTruthSource sends an offer to this channel upon removal)
                 */
                offerTruthSourceForMonitoredTxn.offersChannel.receive()
                val removedEvent = offerCanceledEventRepositoryForMonitoredTxn.removedEventChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assertTrue(offerTruthSourceForMonitoredTxn.offers.isEmpty())
                assertNull(offerTruthSourceForMonitoredTxn.offers[expectedOfferId])
                assertFalse(offer.isCreated.value)
                assertEquals(OfferState.CANCELED, offer.state)
                assertEquals(CancelingOfferState.COMPLETED, offer.cancelingOfferState.value)
                assertNull(blockchainServiceForMonitoredTxn
                    .getMonitoredTransaction(secondTestingServerResponse.offerCancellationTransactionHash))
                assertEquals(secondTestingServerResponse.offerCancellationTransactionHash,
                    offer.offerCancellationTransaction!!.transactionHash)
                assertEquals(expectedOfferId, appendedEvent.offerID)
                assertEquals(expectedOfferId, removedEvent.offerID)
                assertNull(databaseService.getOffer(encoder.encodeToString(expectedOfferIdByteArray)))
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        /*
        OfferService should call the sendTakerInformation method of this class, passing a UUID equal to newOfferID
        to begin the process of sending taker information
         */
        class TestSwapService(val expectedSwapIDForMessage: UUID): SwapNotifiable {
            val chainIDChannel = Channel<BigInteger>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean {
                return if (swapID == expectedSwapIDForMessage) {
                    chainIDChannel.send(chainID)
                    true
                } else {
                    false
                }
            }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {}
            override suspend fun handleSwapFilledEvent(event: SwapFilledEvent) {}
            override suspend fun handlePaymentSentEvent(event: PaymentSentEvent) {}
            override suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent) {}
            override suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent) {}
            override suspend fun handleSellerClosedEvent(event: SellerClosedEvent) {}
        }
        val swapService = TestSwapService(expectedSwapIDForMessage = newOfferID)

        val offerService = OfferService(
            databaseService,
            keyManagerService,
            swapService,
        )
        offerService.setOfferTruthSource(TestOfferTruthSource())

        val exceptionHandler = TestBlockchainExceptionHandler()

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = swapService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        blockchainService.listen()

        withTimeout(60_000) {
            val receivedChainID = swapService.chainIDChannel.receive()
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
        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val offerTruthSource = TestOfferTruthSource()

        // OfferService should call the handleNewSwap method of this class, passing an offer with an ID equal to
        // offerID. We need this new TestSwapService declaration to track when handleNewSwap is called.
        class TestSwapService: SwapNotifiable {
            val offerChannel = Channel<Offer>()
            override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean
            { return false }
            override suspend fun handleFailedTransaction(
                transaction: BlockchainTransaction,
                exception: BlockchainTransactionException
            ) {}
            override suspend fun handleNewSwap(takenOffer: Offer) {
                offerChannel.send(takenOffer)
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
            interfaceID = keyPair.interfaceId,
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
            state = OfferState.OPEN_OFFER_TRANSACTION_SENT
        )
        offerTruthSource.addOffer(offer)
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(newOfferID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
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
            val receivedOffer = swapService.offerChannel.receive()
            assertEquals(newOfferID, receivedOffer.id)
            assertEquals(BigInteger.valueOf(31337L), receivedOffer.chainID)
            assertFalse(exceptionHandler.gotError)
        }

    }

    /**
     * Ensures that [OfferService] handles
     * [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited) events properly for
     * offers not made by the interface user.
     */
    @Test
    fun testHandleOfferEditedEventForUserIsNotMaker() {
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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

        val offer = Offer.fromOnChainData(
            isCreated = true,
            isTaken = false,
            id = expectedOfferId,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            onChainDirection = BigInteger.ZERO,
            onChainSettlementMethods = listOf(),
            protocolVersion = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )
        offerTruthSource.addOffer(offer)
        val encoder = Base64.getEncoder()
        val offerIDB64String = encoder.encodeToString(offer.id.asByteArray())
        val offerForDatabase = DatabaseOffer(
            id = offerIDB64String,
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)
        }

        val exceptionHandler = TestBlockchainExceptionHandler()

        BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        val event = OfferEditedEvent(
            offerID = expectedOfferId,
            chainID = BigInteger.valueOf(31337L),
            transactionHash = "offer_editing_tx_hash",
        )

        runBlocking {
            launch {
                offerService.handleOfferEditedEvent(event)
            }
            withTimeout(60_000) {
                offerEditedEventRepository.removedEventChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assertEquals(expectedOfferId, offerEditedEventRepository.appendedEvent!!.offerID)
                assertEquals(expectedOfferId, offerEditedEventRepository.removedEvent!!.offerID)

                assertEquals(2, offer.settlementMethods.count())
                assertEquals("EUR", offer.settlementMethods[0].currency)
                assertEquals("0.98", offer.settlementMethods[0].price)
                assertEquals("SEPA", offer.settlementMethods[0].method)
                assertEquals("BSD", offer.settlementMethods[1].currency)
                assertEquals("1.00", offer.settlementMethods[1].price)
                assertEquals("SANDDOLLAR", offer.settlementMethods[1].method)

                val settlementMethodsInDatabase = databaseService.getOfferSettlementMethods(
                    offerID = offerIDB64String,
                    chainID = offerForDatabase.chainID
                )
                assertEquals(settlementMethodsInDatabase!!.size, 2)
                assertEquals("{\"f\":\"EUR\",\"p\":\"0.98\",\"m\":\"SEPA\"}",
                    settlementMethodsInDatabase[0].first)
                assertEquals("{\"f\":\"BSD\",\"p\":\"1.00\",\"m\":\"SANDDOLLAR\"}",
                    settlementMethodsInDatabase[1].first)
            }
        }
    }

    /**
     * Ensures that [OfferService] handles
     * [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited) events properly for
     * offers made by the interface user.
     */
    @Test
    fun testHandleOfferEditedEventForUserIsMaker() {
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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

        val offer = Offer.fromOnChainData(
            isCreated = true,
            isTaken = false,
            id = expectedOfferId,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.ZERO,
            amountUpperBound = BigInteger.ZERO,
            securityDepositAmount = BigInteger.ZERO,
            serviceFeeRate = BigInteger.ZERO,
            onChainDirection = BigInteger.ZERO,
            onChainSettlementMethods = listOf(),
            protocolVersion = BigInteger.ZERO,
            chainID = BigInteger.valueOf(31337L),
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OFFER_OPENED
        )
        offer.updateSettlementMethods(listOf(
            SettlementMethod(currency = "USD", price = "1.00", method = "SWIFT", privateData = "some_swift_data")
        ))
        offer.editingOfferState.value = EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION
        offer.offerEditingTransaction = BlockchainTransaction(
            transactionHash = "0xoffer_editing_tx_hash",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.EDIT_OFFER
        )
        offerTruthSource.addOffer(offer)
        val encoder = Base64.getEncoder()
        val offerIDB64String = encoder.encodeToString(offer.id.asByteArray())
        val offerForDatabase = DatabaseOffer(
            id = offerIDB64String,
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = offer.state.asString,
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)
        }
        val serializedSettlementMethodsAndPrivateDetails = offer.settlementMethods.map {
            Pair(Json.encodeToString(it), it.privateData)
        }
        runBlocking {
            databaseService.storeOfferSettlementMethods(
                offerID = encoder.encodeToString(expectedOfferId.asByteArray()),
                chainID = offerForDatabase.chainID,
                settlementMethods = serializedSettlementMethodsAndPrivateDetails
            )
        }

        val pendingSettlementMethods = listOf(
            SettlementMethod(
                currency = "EUR",
                price = "0.98",
                method = "SEPA",
                privateData = "some_sepa_data"
            ),
            SettlementMethod(
                currency = "BSD",
                price = "1.00",
                method = "SANDDOLLAR",
                privateData = "some_sanddollar_data"
            ),
        )
        offer.selectedSettlementMethods.addAll(pendingSettlementMethods)
        val serializedPendingSettlementMethodsAndPrivateDetails = pendingSettlementMethods.map {
            Pair(Json.encodeToString(it), it.privateData)
        }
        runBlocking {
            databaseService.storePendingOfferSettlementMethods(
                offerID = offerIDB64String,
                chainID = offerForDatabase.chainID,
                pendingSettlementMethods = serializedPendingSettlementMethodsAndPrivateDetails
            )
        }

        val exceptionHandler = TestBlockchainExceptionHandler()

        BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = offerService,
            swapService = TestSwapService(),
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )

        val event = OfferEditedEvent(
            offerID = expectedOfferId,
            chainID = BigInteger.valueOf(31337L),
            transactionHash = "offer_editing_tx_hash",
        )

        runBlocking {
            launch {
                offerService.handleOfferEditedEvent(event)
            }
            withTimeout(60_000) {
                offerEditedEventRepository.removedEventChannel.receive()
                assertFalse(exceptionHandler.gotError)
                assertEquals(1, offerTruthSource.offers.size)
                assertEquals(expectedOfferId, offerTruthSource.offers[expectedOfferId]!!.id)
                assertEquals(expectedOfferId, offerEditedEventRepository.appendedEvent!!.offerID)
                assertEquals(expectedOfferId, offerEditedEventRepository.removedEvent!!.offerID)
                assertEquals(EditingOfferState.COMPLETED, offer.editingOfferState.value)
                assertEquals(0, offer.selectedSettlementMethods.size)

                val settlementMethodsInDatabase = databaseService.getOfferSettlementMethods(
                    offerID = offerIDB64String,
                    chainID = offerForDatabase.chainID
                )
                assertEquals(settlementMethodsInDatabase!!.size, 2)
                assertEquals("{\"f\":\"EUR\",\"m\":\"SEPA\",\"p\":\"0.98\"}",
                    settlementMethodsInDatabase[0].first)
                assertEquals("some_sepa_data",
                    settlementMethodsInDatabase[0].second)
                assertEquals("{\"f\":\"BSD\",\"m\":\"SANDDOLLAR\",\"p\":\"1.00\"}",
                    settlementMethodsInDatabase[1].first)
                assertEquals("some_sanddollar_data",
                    settlementMethodsInDatabase[1].second)

                val pendingSettlementMethodsInDatabase = databaseService.getPendingOfferSettlementMethods(
                    offerID = offerIDB64String,
                    chainID = offerForDatabase.chainID
                )
                assertNull(pendingSettlementMethodsInDatabase)
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
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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
     * Ensures that [OfferService.createApproveTokenTransferToOpenOfferTransaction],
     * [BlockchainService.createApproveTransferTransaction], [OfferService.approveTokenTransferToOpenOffer],
     * [OfferService.createOpenOfferTransaction], [BlockchainService.createOpenOfferTransaction],
     * [OfferService.openOffer], and [BlockchainService.sendTransaction]
     * function properly.
     */
    @Test
    fun testOpenOffer() = runBlocking {
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
        val testingServerResponse: TestingServerResponse = testingServerClient.get(testingServiceUrl).body()

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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

        val settlementMethods = listOf(
            SettlementMethod(
                currency = "USD",
                method = "SWIFT",
                price = "1.00",
                privateData = Json.encodeToString(
                    PrivateSWIFTData(
                        accountHolder = "account_holder",
                        bic = "bic",
                        accountNumber = "account_number"
                    )
                ),
            )
        )

        val tokenTransferApprovalTransaction = offerService.createApproveTokenTransferToOpenOfferTransaction(
            stablecoin = testingServerResponse.stablecoinAddress,
            stablecoinInformation = StablecoinInformation(
                currencyCode = "STBL",
                name = "Basic Stablecoin",
                decimal = 3
            ),
            minimumAmount = BigDecimal("100.00"),
            maximumAmount = BigDecimal("200.00"),
            securityDepositAmount = BigDecimal("20.00"),
            direction =  OfferDirection.SELL,
            settlementMethods = settlementMethods
        )

        offerService.approveTokenTransferToOpenOffer(
            chainID = BigInteger.valueOf(31337),
            stablecoin = testingServerResponse.stablecoinAddress,
            stablecoinInformation = StablecoinInformation(
                currencyCode = "STBL",
                name = "Basic Stablecoin",
                decimal = 3
            ),
            minimumAmount = BigDecimal("100.00"),
            maximumAmount = BigDecimal("200.00"),
            securityDepositAmount = BigDecimal("20.00"),
            direction =  OfferDirection.SELL,
            settlementMethods = settlementMethods,
            approveTokenTransferToOpenOfferTransaction = tokenTransferApprovalTransaction
        )
        // Since we aren't parsing new events, we have to set the offer state manually
        offerTruthSource.offers.entries.first().value.state = OfferState.AWAITING_OPENING

        val offerOpeningTransaction = offerService.createOpenOfferTransaction(
            offer = offerTruthSource.offers.entries.first().value,
        )

        offerService.openOffer(
            offer = offerTruthSource.offers.entries.first().value,
            offerOpeningTransaction = offerOpeningTransaction
        )

        val offer = offerTruthSource.offers.entries.first().value
        assert(offer.isCreated.value)
        assertFalse(offer.isTaken.value)
        // TODO: check proper maker address once WalletService is implemented
        assertEquals(testingServerResponse.stablecoinAddress, offer.stablecoin)
        assertEquals(BigInteger.valueOf(100_000), offer.amountLowerBound)
        assertEquals(BigInteger.valueOf(200_000), offer.amountUpperBound)
        assertEquals(BigInteger.valueOf(20_000), offer.securityDepositAmount)
        assertEquals(BigInteger.valueOf(100), offer.serviceFeeRate)
        assertEquals(BigInteger.valueOf(1_000), offer.serviceFeeAmountLowerBound)
        assertEquals(BigInteger.valueOf(2_000), offer.serviceFeeAmountUpperBound)
        assertEquals(BigInteger.ONE, offer.onChainDirection)
        assertEquals(OfferDirection.SELL, offer.direction)
        assertEquals(settlementMethods.size, offer.onChainSettlementMethods.size)
        for (index in offer.onChainSettlementMethods.indices) {
            assert(
                offer.onChainSettlementMethods[index].contentEquals(
                    Json.encodeToString(settlementMethods[index]).encodeToByteArray()
                )
            )
        }
        assertEquals(settlementMethods.size, offer.settlementMethods.size)
        for (index in offer.settlementMethods.indices) {
            assertEquals(offer.settlementMethods[index], settlementMethods[index])
        }
        assertEquals(BigInteger.ZERO, offer.protocolVersion)
        assert(offer.havePublicKey)

        assertNotNull(keyManagerService.getKeyPair(offer.interfaceID))

        val encoder = Base64.getEncoder()
        val offerInDatabase = databaseService.getOffer(encoder.encodeToString(offer.id.asByteArray()))!!
        //TODO: check proper maker address once WalletService is implemented
        val mostlyExpectedOfferInDatabase = DatabaseOffer(
            id = encoder.encodeToString(offer.id.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = encoder.encodeToString(offer.interfaceID),
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound.toString(),
            amountUpperBound = offer.amountUpperBound.toString(),
            securityDepositAmount = offer.securityDepositAmount.toString(),
            serviceFeeRate = offer.serviceFeeRate.toString(),
            onChainDirection = offer.onChainDirection.toString(),
            protocolVersion = offer.protocolVersion.toString(),
            chainID = offer.chainID.toString(),
            havePublicKey = 1L,
            isUserMaker = 1L,
            state = OfferState.OPEN_OFFER_TRANSACTION_SENT.asString,
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        assertEquals(mostlyExpectedOfferInDatabase.id, offerInDatabase.id)
        assertEquals(mostlyExpectedOfferInDatabase.isCreated, offerInDatabase.isCreated)
        assert(mostlyExpectedOfferInDatabase.interfaceId.contentEquals(offerInDatabase.interfaceId))
        assertEquals(mostlyExpectedOfferInDatabase.stablecoin, offerInDatabase.stablecoin)
        assertEquals(mostlyExpectedOfferInDatabase.amountLowerBound, offerInDatabase.amountLowerBound)
        assertEquals(mostlyExpectedOfferInDatabase.amountUpperBound, offerInDatabase.amountUpperBound)
        assertEquals(mostlyExpectedOfferInDatabase.securityDepositAmount, offerInDatabase.securityDepositAmount)
        assertEquals(mostlyExpectedOfferInDatabase.serviceFeeRate, offerInDatabase.serviceFeeRate)
        assertEquals(mostlyExpectedOfferInDatabase.onChainDirection, offerInDatabase.onChainDirection)
        assertEquals(mostlyExpectedOfferInDatabase.protocolVersion, offerInDatabase.protocolVersion)
        assertEquals(mostlyExpectedOfferInDatabase.chainID, offerInDatabase.chainID)
        assertEquals(mostlyExpectedOfferInDatabase.havePublicKey, offerInDatabase.havePublicKey)
        assertEquals(mostlyExpectedOfferInDatabase.isUserMaker, offerInDatabase.isUserMaker)

        val settlementMethodsInDatabase = databaseService.getOfferSettlementMethods(
            offerID = encoder.encodeToString(offer.id.asByteArray()),
            chainID = offer.chainID.toString()
        )!!
        val expectedSettlementMethodsInDatabase = offer.onChainSettlementMethods.map {
            encoder.encodeToString(it)
        }
        for (index in expectedSettlementMethodsInDatabase.indices) {
            assertEquals(
                expectedSettlementMethodsInDatabase[index],
                settlementMethodsInDatabase[index].first
            )
        }

        val offerStructOnChain = blockchainService.getOffer(offer.id)!!
        assert(offerStructOnChain.isCreated)
        assertFalse(offerStructOnChain.isTaken)
        // TODO: check proper maker address once WalletService is implemented
        assert(offerStructOnChain.interfaceID.contentEquals(offer.interfaceID))
        assertEquals(offerStructOnChain.stablecoin.lowercase(), offer.stablecoin.lowercase())
        assertEquals(offerStructOnChain.amountLowerBound, offer.amountLowerBound)
        assertEquals(offerStructOnChain.amountUpperBound, offer.amountUpperBound)
        assertEquals(offerStructOnChain.securityDepositAmount, offer.securityDepositAmount)
        assertEquals(offerStructOnChain.serviceFeeRate, offer.serviceFeeRate)
        assertEquals(offerStructOnChain.direction, offer.onChainDirection)
        assertEquals(offerStructOnChain.settlementMethods.size, offer.onChainSettlementMethods.size)
        for (index in offerStructOnChain.settlementMethods.indices) {
            assert(
                offerStructOnChain.settlementMethods[index].contentEquals(
                    offer.onChainSettlementMethods[index]
                )
            )
        }
        assertEquals(offerStructOnChain.protocolVersion, offer.protocolVersion)
        // TODO: re-enable this once we get accurate chain IDs
        assertEquals(offerStructOnChain.chainID, offer.chainID)

    }

    /**
     * Ensures that [OfferService.createCancelOfferTransaction], [OfferService.cancelOffer] and
     * [BlockchainService.sendTransaction] function properly.
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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
            interfaceID = ByteArray(0),
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
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )

        runBlocking {
            databaseService.storeOffer(offerForDatabase)

            val offerCancellationTransaction = offerService.createCancelOfferTransaction(
                offer = offer
            )

            offerService.cancelOffer(
                offer = offer,
                offerCancellationTransaction = offerCancellationTransaction
            )

            assertEquals(CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION, offerTruthSource
                .offers[offerID]?.cancelingOfferState?.value)
            assertNotNull(offerTruthSource.offers[offerID]?.offerCancellationTransaction?.transactionHash)

            val offerInDatabase = databaseService.getOffer(offerIDString)
            assertEquals(CancelingOfferState.AWAITING_TRANSACTION_CONFIRMATION.asString, offerInDatabase!!
                .cancelingOfferState)
            assertNotNull(offerInDatabase.offerCancellationTransactionHash)

            val offerStruct = blockchainService.getOffer(offerID)
            assertNull(offerStruct)
        }

    }

    /**
     * Ensures that [OfferService.createEditOfferTransaction], [OfferService.editOffer] and
     * [BlockchainService.editOfferAsync] function properly.
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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
            interfaceID = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
            amountLowerBound = BigInteger.valueOf(10_000L) * BigInteger.TEN.pow(18),
            amountUpperBound = BigInteger.valueOf(20_000L) * BigInteger.TEN.pow(18),
            securityDepositAmount = BigInteger.valueOf(2_000L) * BigInteger.TEN.pow(18),
            serviceFeeRate = BigInteger.valueOf(100L),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(
                SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA"
                )
            ),
            protocolVersion = BigInteger.ONE,
            chainID = BigInteger.valueOf(31337L), // Hardhat blockchain ID
            havePublicKey = true,
            isUserMaker = true,
            state = OfferState.OFFER_OPENED
        )
        offerTruthSource.offers[offerID] = offer
        val encoder = Base64.getEncoder()
        val offerForDatabase = DatabaseOffer(
            id = encoder.encodeToString(offerID.asByteArray()),
            isCreated = 1L,
            isTaken = 0L,
            maker = offer.maker,
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )

        offer.editingOfferState.value = EditingOfferState.VALIDATING

        runBlocking {
            databaseService.storeOffer(offerForDatabase)

            val offerEditingTransaction = offerService.createEditOfferTransaction(
                offer = offer,
                newSettlementMethods = listOf(
                    SettlementMethod(
                        currency = "USD",
                        price = "1.00",
                        method = "SWIFT",
                        privateData = "some_swift_data",
                    )
                ),
            )

            offerService.editOffer(
                offer = offer,
                newSettlementMethods = listOf(
                    SettlementMethod(
                        currency = "USD",
                        price = "1.00",
                        method = "SWIFT",
                        privateData = "some_swift_data",
                    )
                ),
                offerEditingTransaction = offerEditingTransaction,
            )

            assertEquals(EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION, offer.editingOfferState.value)
            assertNotNull(offer.offerEditingTransaction?.transactionHash)
            val expectedSettlementMethods = listOf(
                SettlementMethod(
                    currency = "USD",
                    method = "SWIFT",
                    price = "1.00"
                )
            )
            val offerInDatabase = databaseService.getOffer(encoder.encodeToString(offer.id.asByteArray()))
            assertEquals(EditingOfferState.AWAITING_TRANSACTION_CONFIRMATION.asString, offerInDatabase!!
                .editingOfferState)
            assertNotNull(offerInDatabase.offerEditingTransactionHash)
            val offerStruct = blockchainService.getOffer(offerID)
            assertEquals(expectedSettlementMethods.size, offerStruct!!.settlementMethods.size)
            offerStruct.settlementMethods.indices.forEach {
                assert(expectedSettlementMethods[it].onChainData!!.contentEquals(offerStruct.settlementMethods[it]))
            }
            val pendingSettlementMethodsInDatabase = databaseService.getPendingOfferSettlementMethods(
                offerID = encoder.encodeToString(offerID.asByteArray()),
                chainID = BigInteger.valueOf(31337L).toString()
            )
            assertEquals(1, pendingSettlementMethodsInDatabase!!.size)
            assertEquals(Json.encodeToString(expectedSettlementMethods[0]), pendingSettlementMethodsInDatabase[0].first)
            assertEquals("some_swift_data", pendingSettlementMethodsInDatabase[0].second)
        }
    }

    /**
     * Ensures that [OfferService.takeOffer], [BlockchainService.approveTokenTransferAsync] and
     * [BlockchainService.takeOfferAsync] function properly.
     */
    @Test
    fun testTakeOffer() {

        val offerID = UUID.randomUUID()

        val settlementMethodString = Json.encodeToString(
            SettlementMethod(
                currency = "EUR",
                price = "0.98",
                method = "SEPA",
            )
        )

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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

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
            interfaceID = ByteArray(0),
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
            interfaceId = encoder.encodeToString(offer.interfaceID),
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
            approveToOpenState = offer.approvingToOpenState.value.asString,
            approveToOpenTransactionHash = null,
            approveToOpenTransactionCreationTime = null,
            approveToOpenTransactionCreationBlockNumber = null,
            openingOfferState = offer.openingOfferState.value.asString,
            openingOfferTransactionHash = null,
            openingOfferTransactionCreationTime = null,
            openingOfferTransactionCreationBlockNumber = null,
            cancelingOfferState = offer.cancelingOfferState.value.asString,
            offerCancellationTransactionHash = null,
            offerCancellationTransactionCreationTime = null,
            offerCancellationTransactionCreationBlockNumber = null,
            editingOfferState = offer.editingOfferState.value.asString,
            offerEditingTransactionHash = null,
            offerEditingTransactionCreationTime = null,
            offerEditingTransactionCreationBlockNumber = null,
        )
        runBlocking {
            databaseService.storeOffer(offerForDatabase)

            val offerInDatabaseBeforeTaking = databaseService.getOffer(id = offerIDB64String)
            assertEquals(offerForDatabase, offerInDatabaseBeforeTaking)

            val swapData = ValidatedNewSwapData(
                takenSwapAmount = BigInteger.valueOf(15_000L) * BigInteger.TEN.pow(18),
                makerSettlementMethod = SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                    privateData = "EUR_SEPA_Private_Data"
                ),
                takerSettlementMethod = SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                    privateData = "Taker_EUR_SEPA_Private_Data"
                )
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
            assert(Json.encodeToString(
                SettlementMethod(
                    currency = "EUR",
                    price = "0.98",
                    method = "SEPA",
                )
            ).encodeToByteArray().contentEquals(swapInTruthSource.onChainSettlementMethod))
            assertEquals("EUR", swapInTruthSource.settlementMethod.currency)
            assertEquals("0.98", swapInTruthSource.settlementMethod.price)
            assertEquals("SEPA", swapInTruthSource.settlementMethod.method)
            assertEquals("Taker_EUR_SEPA_Private_Data", swapInTruthSource.takerPrivateSettlementMethodData)
            assertEquals(BigInteger.ONE, swapInTruthSource.protocolVersion)
            assertFalse(swapInTruthSource.isPaymentSent)
            assertFalse(swapInTruthSource.isPaymentReceived)
            assertFalse(swapInTruthSource.hasBuyerClosed)
            assertFalse(swapInTruthSource.hasSellerClosed)
            assertEquals(BigInteger.ZERO, swapInTruthSource.onChainDisputeRaiser)
            assertEquals(BigInteger.valueOf(31337L), swapInTruthSource.chainID)
            assertEquals(SwapState.TAKE_OFFER_TRANSACTION_BROADCAST, swapInTruthSource.state.value)

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
                settlementMethod = encoder.encodeToString(Json.encodeToString(
                    SettlementMethod(
                        currency = "EUR",
                        price = "0.98",
                        method = "SEPA",
                    ),
                ).encodeToByteArray()),
                makerPrivateData = null,
                makerPrivateDataInitializationVector = null,
                takerPrivateData = "Taker_EUR_SEPA_Private_Data",
                takerPrivateDataInitializationVector = null,
                protocolVersion = swapInTruthSource.protocolVersion.toString(),
                isPaymentSent = 0L,
                isPaymentReceived = 0L,
                hasBuyerClosed = 0L,
                hasSellerClosed = 0L,
                disputeRaiser = swapInTruthSource.onChainDisputeRaiser.toString(),
                chainID = swapInTruthSource.chainID.toString(),
                state = swapInTruthSource.state.value.asString,
                role = swapInTruthSource.role.asString,
                reportPaymentSentState = swapInTruthSource.reportingPaymentSentState.value.asString,
                reportPaymentSentTransactionHash = null,
                reportPaymentSentTransactionCreationTime = null,
                reportPaymentSentTransactionCreationBlockNumber = null,
                reportPaymentReceivedState = swapInTruthSource.reportingPaymentReceivedState.value.asString,
                reportPaymentReceivedTransactionHash = null,
                reportPaymentReceivedTransactionCreationTime = null,
                reportPaymentReceivedTransactionCreationBlockNumber = null,
                closeSwapState = swapInTruthSource.closingSwapState.value.asString,
                closeSwapTransactionHash = null,
                closeSwapTransactionCreationTime = null,
                closeSwapTransactionCreationBlockNumber = null,
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
            assertEquals(expectedSwapInDatabase.takerPrivateData, swapInDatabase.takerPrivateData)
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

            // Check that the offer's settlement methods have been deleted from persistent storage
            assertNull(databaseService.getOfferSettlementMethods(
                offerID = offerIDB64String,
                chainID = BigInteger.valueOf(31337L).toString()),
            )
        }

    }

}