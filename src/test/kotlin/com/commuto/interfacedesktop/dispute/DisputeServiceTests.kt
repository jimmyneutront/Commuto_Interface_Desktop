package com.commuto.interfacedesktop.dispute

import com.commuto.interfacedesktop.CommutoWeb3j
import com.commuto.interfacedesktop.blockchain.*
import com.commuto.interfacedesktop.blockchain.events.commutoswap.DisputeRaisedEvent
import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.db.Swap as DatabaseSwap
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.TestOfferService
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.TestOfferMessageNotifiable
import com.commuto.interfacedesktop.p2p.TestP2PExceptionHandler
import com.commuto.interfacedesktop.p2p.TestSwapMessageNotifiable
import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncementAsUserForDispute
import com.commuto.interfacedesktop.swap.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Tests for [DisputeService]
 */
class DisputeServiceTests {

    /**
     * Ensure [DisputeService] properly handles failed transactions that raise disputes for swaps.
     */
    @Test
    fun testHandleFailedRaiseDisputeTransaction() = runBlocking {
        val swapID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val swapTruthSource = TestSwapTruthSource()

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "0x0000000000000000000000000000000000000000",
            makerInterfaceID = ByteArray(0),
            taker = "0x0000000000000000000000000000000000000000",
            takerInterfaceID = ByteArray(0),
            stablecoin = "0x0000000000000000000000000000000000000000",
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
            state = SwapState.AWAITING_PAYMENT_SENT,
            role = SwapRole.MAKER_AND_BUYER,
        )
        swap.raisingDisputeState.value = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION
        val raisingDisputeTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.RAISE_DISPUTE
        )
        swap.raisingDisputeTransaction = raisingDisputeTransaction
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
            role = swap.role.asString,
            approveToFillState = swap.approvingToFillState.value.asString,
            approveToFillTransactionHash = null,
            approveToFillTransactionCreationTime = null,
            approveToFillTransactionCreationBlockNumber = null,
            fillingSwapState = swap.fillingSwapState.value.asString,
            fillingSwapTransactionHash = null,
            fillingSwapTransactionCreationTime = null,
            fillingSwapTransactionCreationBlockNumber = null,
            reportPaymentSentState = swap.reportingPaymentSentState.value.asString,
            reportPaymentSentTransactionHash = null,
            reportPaymentSentTransactionCreationTime = null,
            reportPaymentSentTransactionCreationBlockNumber = null,
            reportPaymentReceivedState = swap.reportingPaymentReceivedState.value.asString,
            reportPaymentReceivedTransactionHash = null,
            reportPaymentReceivedTransactionCreationTime = null,
            reportPaymentReceivedTransactionCreationBlockNumber = null,
            closeSwapState = swap.closingSwapState.value.asString,
            closeSwapTransactionHash = null,
            closeSwapTransactionCreationTime = null,
            closeSwapTransactionCreationBlockNumber = null,
            disputeState = swap.disputeState.value.asString,
            raisingDisputeState = swap.raisingDisputeState.value.asString,
            raisingDisputeTransactionHash = null,
            raisingDisputeTransactionCreationTime = null,
            raisingDisputeTransactionCreationBlockNumber = null,
        )
        databaseService.storeSwap(swapForDatabase)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService,
        )
        disputeService.setSwapTruthSource(swapTruthSource)

        disputeService.handleFailedTransaction(
            transaction = raisingDisputeTransaction,
            exception = BlockchainTransactionException(message = "tx failed"),
        )

        assertEquals(RaisingDisputeState.EXCEPTION, swap.raisingDisputeState.value)
        assertNotNull(swap.raisingDisputeException)
        val swapInDatabase = databaseService.getSwap(id = encoder.encodeToString(swapID.asByteArray()))
        assertEquals(RaisingDisputeState.EXCEPTION.asString, swapInDatabase?.raisingDisputeState)
    }

    /**
     * Ensures that [DisputeService] properly handles [DisputeRaisedEvent]s for disputes raised by the user.
     */
    @Test
    fun testHandleDisputeRaisedEventForUserIsDisputeRaiser() = runBlocking {
        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_disputeservice_handleDisputeRaisedEvent"
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
                    parameters.append("events", "offer-opened-taken-DisputeRaised")
                    parameters.append("swapID", swapID.toString())
                    parameters.append("isUserCounterparty", "False")
                }
            }.body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val swapTruthSource = TestSwapTruthSource()
        disputeService.setSwapTruthSource(swapTruthSource)

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val userKeyPair = keyManagerService.generateKeyPair()

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = userKeyPair.interfaceId,
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
            state = SwapState.AWAITING_PAYMENT_SENT,
            role = SwapRole.MAKER_AND_BUYER,
        )
        swap.raisingDisputeState.value = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION
        val disputeRaisingTransaction = BlockchainTransaction(
            transactionHash = "a_transaction_hash_here",
            timeOfCreation = Date(),
            latestBlockNumberAtCreation = BigInteger.ZERO,
            type = BlockchainTransactionType.RAISE_DISPUTE
        )
        swap.raisingDisputeTransaction = disputeRaisingTransaction
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
            role = swap.role.asString,
            approveToFillState = swap.approvingToFillState.value.asString,
            approveToFillTransactionHash = null,
            approveToFillTransactionCreationTime = null,
            approveToFillTransactionCreationBlockNumber = null,
            fillingSwapState = swap.fillingSwapState.value.asString,
            fillingSwapTransactionHash = null,
            fillingSwapTransactionCreationTime = null,
            fillingSwapTransactionCreationBlockNumber = null,
            reportPaymentSentState = swap.reportingPaymentSentState.value.asString,
            reportPaymentSentTransactionHash = null,
            reportPaymentSentTransactionCreationTime =  null,
            reportPaymentSentTransactionCreationBlockNumber = null,
            reportPaymentReceivedState = swap.reportingPaymentReceivedState.value.asString,
            reportPaymentReceivedTransactionHash = null,
            reportPaymentReceivedTransactionCreationTime =  null,
            reportPaymentReceivedTransactionCreationBlockNumber = null,
            closeSwapState = swap.closingSwapState.value.asString,
            closeSwapTransactionHash = null,
            closeSwapTransactionCreationTime = null,
            closeSwapTransactionCreationBlockNumber = null,
            disputeState = swap.disputeState.value.asString,
            raisingDisputeState = swap.raisingDisputeState.value.asString,
            raisingDisputeTransactionHash = null,
            raisingDisputeTransactionCreationTime = null,
            raisingDisputeTransactionCreationBlockNumber = null,
        )
        databaseService.storeSwap(swapForDatabase)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var id: String? = null
            var chainID: String? = null
            var userKeyPair: KeyPair? = null
            override suspend fun announcePublicKeyAsUserForDispute(
                id: String,
                chainID: String,
                keyPair: KeyPair
            ) {
                this.id = id
                this.chainID = chainID
                this.userKeyPair = keyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val event = DisputeRaisedEvent(
            swapID = swapID,
            disputeAgent0 = "0x_dispute_agent_0",
            disputeAgent1 = "0x_dispute_agent_1",
            disputeAgent2 = "0x_dispute_agent_2",
            chainID = BigInteger.valueOf(31337),
            transactionHash = "0xa_transaction_hash_here",
        )

        disputeService.handleDisputeRaisedEvent(event = event)

        assertEquals(swapID.toString(), p2pService.id)
        assertEquals(swap.chainID.toString(), p2pService.chainID)
        assert(swap.makerInterfaceID.contentEquals(p2pService.userKeyPair?.interfaceId))
        assertEquals(DisputeState.SENT_PKA, swap.disputeState.value)
        assertEquals(RaisingDisputeState.COMPLETED, swap.raisingDisputeState.value)
        val swapInDatabase = databaseService.getSwap(swapForDatabase.id)
        assertEquals(DisputeState.SENT_PKA.asString, swapInDatabase?.disputeState)
        assertEquals(RaisingDisputeState.COMPLETED.asString, swapInDatabase?.raisingDisputeState)
    }

    /**
     * Ensures that [DisputeService] properly handles [DisputeRaisedEvent]s for disputes raised by the user's
     * counterparty.
     */
    @Test
    fun testHandleDisputeRaisedEventForUserIsDisputeRaisingCounterparty() = runBlocking {
        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_disputeservice_handleDisputeRaisedEvent"
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
                    parameters.append("events", "offer-opened-taken-DisputeRaised")
                    parameters.append("swapID", swapID.toString())
                    parameters.append("isUserCounterparty", "True")
                }
            }.body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val swapTruthSource = TestSwapTruthSource()
        disputeService.setSwapTruthSource(swapTruthSource)

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val userKeyPair = keyManagerService.generateKeyPair()

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = userKeyPair.interfaceId,
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
            state = SwapState.AWAITING_PAYMENT_SENT,
            role = SwapRole.TAKER_AND_SELLER,
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
            role = swap.role.asString,
            approveToFillState = swap.approvingToFillState.value.asString,
            approveToFillTransactionHash = null,
            approveToFillTransactionCreationTime = null,
            approveToFillTransactionCreationBlockNumber = null,
            fillingSwapState = swap.fillingSwapState.value.asString,
            fillingSwapTransactionHash = null,
            fillingSwapTransactionCreationTime = null,
            fillingSwapTransactionCreationBlockNumber = null,
            reportPaymentSentState = swap.reportingPaymentSentState.value.asString,
            reportPaymentSentTransactionHash = null,
            reportPaymentSentTransactionCreationTime =  null,
            reportPaymentSentTransactionCreationBlockNumber = null,
            reportPaymentReceivedState = swap.reportingPaymentReceivedState.value.asString,
            reportPaymentReceivedTransactionHash = null,
            reportPaymentReceivedTransactionCreationTime =  null,
            reportPaymentReceivedTransactionCreationBlockNumber = null,
            closeSwapState = swap.closingSwapState.value.asString,
            closeSwapTransactionHash = null,
            closeSwapTransactionCreationTime = null,
            closeSwapTransactionCreationBlockNumber = null,
            disputeState = swap.disputeState.value.asString,
            raisingDisputeState = swap.raisingDisputeState.value.asString,
            raisingDisputeTransactionHash = null,
            raisingDisputeTransactionCreationTime = null,
            raisingDisputeTransactionCreationBlockNumber = null,
        )
        databaseService.storeSwap(swapForDatabase)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var id: String? = null
            var chainID: String? = null
            var userKeyPair: KeyPair? = null
            override suspend fun announcePublicKeyAsUserForDispute(
                id: String,
                chainID: String,
                keyPair: KeyPair
            ) {
                this.id = id
                this.chainID = chainID
                this.userKeyPair = keyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val event = DisputeRaisedEvent(
            swapID = swapID,
            disputeAgent0 = "0x_dispute_agent_0",
            disputeAgent1 = "0x_dispute_agent_1",
            disputeAgent2 = "0x_dispute_agent_2",
            chainID = BigInteger.valueOf(31337),
            transactionHash = "0xa_transaction_hash_here",
        )

        disputeService.handleDisputeRaisedEvent(event = event)
        assertEquals(swapID.toString(), p2pService.id)
        assertEquals(swap.chainID.toString(), p2pService.chainID)
        assert(swap.takerInterfaceID.contentEquals(p2pService.userKeyPair?.interfaceId))
        assertEquals(DisputeState.SENT_PKA, swap.disputeState.value)
        val swapInDatabase = databaseService.getSwap(swapForDatabase.id)
        assertEquals(DisputeState.SENT_PKA.asString, swapInDatabase?.disputeState)

    }

    /**
     * Ensures that [DisputeService] properly handles [DisputeRaisedEvent]s for disputes in which the user is the first
     * dispute agent.
     */
    @Test
    fun testHandleDisputeRaisedEventForUserIsFirstDisputeAgent(): Unit = runBlocking {
        val swapID = UUID.randomUUID()

        @Serializable
        data class TestingServerResponse(val commutoSwapAddress: String, val stablecoinAddress: String)
        val testingServiceUrl = "http://localhost:8546/test_disputeservice_handleDisputeRaisedEvent"
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
                    parameters.append("events", "offer-opened-taken-DisputeRaised")
                    parameters.append("swapID", swapID.toString())
                    parameters.append("isUserCounterparty", "False")
                }
            }.body()
        }

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val disputeTruthSource = TestDisputeTruthSource()
        disputeService.setDisputeTruthSource(disputeTruthSource)

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {

            var disputeAgentKeyPair: KeyPair? = null
            var swapID: UUID? = null
            var disputeRole: DisputeRole? = null
            var disputeAgentEthereumKeyPair: Credentials? = null

            override suspend fun announcePublicKeyAsAgentForDispute(
                keyPair: KeyPair,
                swapId: UUID,
                role: DisputeRole,
                ethereumKeyPair: Credentials
            ) {
                disputeAgentKeyPair = keyPair
                this.swapID = swapId
                disputeRole = role
                disputeAgentEthereumKeyPair = ethereumKeyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val event = DisputeRaisedEvent(
            swapID = swapID,
            disputeAgent0 = blockchainService.getAddress(),
            disputeAgent1 = "0x_dispute_agent_1",
            disputeAgent2 = "0x_dispute_agent_2",
            chainID = BigInteger.valueOf(31337),
            transactionHash = "0xa_transaction_hash_here",
        )

        disputeService.handleDisputeRaisedEvent(event = event)

        val swapAndDisputeInDatabase = databaseService.getSwapAndDispute(id = swapID.toString())
        assertEquals(DisputeRole.DISPUTE_AGENT_0.asString, swapAndDisputeInDatabase?.role)
        assertNotNull(swapAndDisputeInDatabase?.disputeAgent0InterfaceID)
        assertNotNull(databaseService.decryptMakerCommunicationKeyFromSwapAndDispute(swapAndDisputeInDatabase!!))
        assertNotNull(databaseService.decryptTakerCommunicationKeyFromSwapAndDispute(swapAndDisputeInDatabase))
        assertNotNull(databaseService.decryptDisputeAgentCommunicationKeyFromSwapAndDispute(swapAndDisputeInDatabase))
        assertEquals(DisputeStateAsAgent.CREATED_COMMUNICATION_KEYS.asString, swapAndDisputeInDatabase.state)

        val swapAndDispute = disputeTruthSource.swapAndDisputes[swapID]
        assertEquals(DisputeRole.DISPUTE_AGENT_0, swapAndDispute?.role)
        assertNotNull(swapAndDispute?.disputeAgent0InterfaceID)
        assertNotNull(swapAndDispute?.makerCommunicationKey)
        assertNotNull(swapAndDispute?.takerCommunicationKey)
        assertNotNull(swapAndDispute?.disputeAgentCommunicationKey)
        assertEquals(DisputeStateAsAgent.CREATED_COMMUNICATION_KEYS, swapAndDispute?.state?.value)

        assertNotNull(keyManagerService.getKeyPair(swapAndDispute?.disputeAgent0InterfaceID!!))

    }

    /**
     * Ensures that [DisputeService] properly handles [PublicKeyAnnouncementAsUserForDispute] messages sent by the maker
     * when they are the dispute raiser, when the user of this interface is the first dispute agent.
     */
    @Test
    fun testHandlePublicKeyAnnouncementAsUserForDispute_asFirstDisputeAgent_makerDisputeRaiser() = runBlocking {
        val swapID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val disputeTruthSource = TestDisputeTruthSource()
        disputeService.setDisputeTruthSource(disputeTruthSource)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = ""
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {

            var messageType: String? = null
            var id: String? = null
            var chainID: String? = null
            var key: String? = null
            var recipientPublicKey: PublicKey? = null
            var senderKeyPair: KeyPair? = null

            override suspend fun sendCommunicationKey(
                messageType: String,
                id: String,
                chainID: String,
                key: String,
                recipientPublicKey: PublicKey,
                senderKeyPair: KeyPair
            ) {
                this.messageType = messageType
                this.id = id
                this.chainID = chainID
                this.key = key
                this.recipientPublicKey = recipientPublicKey
                this.senderKeyPair = senderKeyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val makerKeyPair = KeyPair()
        val makerCommunicationKey = SymmetricKey()
        val disputeAgent0KeyPair = keyManagerService.generateKeyPair(storeResult=true)
        val swapAndDispute = SwapAndDispute(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = makerKeyPair.interfaceId,
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
            onChainDisputeRaiser = BigInteger.ONE, // Maker is dispute raiser
            chainID = BigInteger.valueOf(31337L),
            disputeRaisedBlockNumber = BigInteger.ZERO,
            disputeAgent0 = blockchainService.getAddress(),
            disputeAgent1 = "",
            disputeAgent2 = "",
            hasDisputeAgent0Proposed = false,
            disputeAgent0MakerPayout = BigInteger.ZERO,
            disputeAgent0TakerPayout = BigInteger.ZERO,
            disputeAgent0ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent1Proposed = false,
            disputeAgent1MakerPayout = BigInteger.ZERO,
            disputeAgent1TakerPayout = BigInteger.ZERO,
            disputeAgent1ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent2Proposed = false,
            disputeAgent2MakerPayout = BigInteger.ZERO,
            disputeAgent2TakerPayout = BigInteger.ZERO,
            disputeAgent2ConfiscationPayout = BigInteger.ZERO,
            onChainMatchingProposals = BigInteger.ZERO,
            makerReaction = BigInteger.ZERO,
            takerReaction = BigInteger.ZERO,
            onChainState = BigInteger.ZERO,
            hasMakerPaidOut = false,
            hasTakerPaidOut = false,
            totalWithoutSpentServiceFees = BigInteger.ZERO,
            role = DisputeRole.DISPUTE_AGENT_0,
        )
        swapAndDispute.makerCommunicationKey = makerCommunicationKey
        swapAndDispute.disputeAgent0InterfaceID = disputeAgent0KeyPair.interfaceId
        disputeTruthSource.swapAndDisputes[swapID] = swapAndDispute
        databaseService.storeSwapAndDispute(swapAndDispute = swapAndDispute.toDatabaseSwapAndDispute())

        val message = PublicKeyAnnouncementAsUserForDispute(
            id = swapID,
            chainID = swapAndDispute.chainID,
            publicKey = makerKeyPair.getPublicKey()
        )

        disputeService.handlePublicKeyAnnouncementAsUserForDispute(message = message)

        val makerPublicKeyInDatabase = keyManagerService.getPublicKey(swapAndDispute.makerInterfaceID)
        assert(makerKeyPair.interfaceId.contentEquals(makerPublicKeyInDatabase!!.interfaceId))
        assertEquals("MCKAnnouncement", p2pService.messageType)
        assertEquals(swapID.toString(), p2pService.id)
        assertEquals(swapAndDispute.chainID.toString(), p2pService.chainID)
        assert(makerCommunicationKey.keyBytes.contentEquals(Base64.getDecoder().decode(p2pService.key)))
        assert(makerKeyPair.interfaceId.contentEquals(p2pService.recipientPublicKey!!.interfaceId))
        assert(disputeAgent0KeyPair.interfaceId.contentEquals(p2pService.senderKeyPair!!.interfaceId))
        val swapAndDisputeInDatabase = databaseService.getSwapAndDispute(id = swapID.toString())
        assertEquals(1L, swapAndDisputeInDatabase!!.sentKeyToMaker)
        assert(swapAndDispute.sentKeyToMaker)
    }

    /**
     * Ensures that [DisputeService] properly handles [PublicKeyAnnouncementAsUserForDispute] messages sent by the taker
     * when they are the dispute raiser, when the user of this interface is the first dispute agent.
     */
    @Test
    fun testHandlePublicKeyAnnouncementAsUserForDispute_asFirstDisputeAgent_takerDisputeRaiser() = runBlocking {
        val swapID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val disputeTruthSource = TestDisputeTruthSource()
        disputeService.setDisputeTruthSource(disputeTruthSource)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = ""
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {

            var messageType: String? = null
            var id: String? = null
            var chainID: String? = null
            var key: String? = null
            var recipientPublicKey: PublicKey? = null
            var senderKeyPair: KeyPair? = null

            override suspend fun sendCommunicationKey(
                messageType: String,
                id: String,
                chainID: String,
                key: String,
                recipientPublicKey: PublicKey,
                senderKeyPair: KeyPair
            ) {
                this.messageType = messageType
                this.id = id
                this.chainID = chainID
                this.key = key
                this.recipientPublicKey = recipientPublicKey
                this.senderKeyPair = senderKeyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val takerKeyPair = KeyPair()
        val takerCommunicationKey = SymmetricKey()
        val disputeAgent0KeyPair = keyManagerService.generateKeyPair(storeResult=true)
        val swapAndDispute = SwapAndDispute(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = ByteArray(0),
            taker = "",
            takerInterfaceID = takerKeyPair.interfaceId,
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
            onChainDisputeRaiser = BigInteger.valueOf(2L), // Taker is dispute raiser
            chainID = BigInteger.valueOf(31337L),
            disputeRaisedBlockNumber = BigInteger.ZERO,
            disputeAgent0 = blockchainService.getAddress(),
            disputeAgent1 = "",
            disputeAgent2 = "",
            hasDisputeAgent0Proposed = false,
            disputeAgent0MakerPayout = BigInteger.ZERO,
            disputeAgent0TakerPayout = BigInteger.ZERO,
            disputeAgent0ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent1Proposed = false,
            disputeAgent1MakerPayout = BigInteger.ZERO,
            disputeAgent1TakerPayout = BigInteger.ZERO,
            disputeAgent1ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent2Proposed = false,
            disputeAgent2MakerPayout = BigInteger.ZERO,
            disputeAgent2TakerPayout = BigInteger.ZERO,
            disputeAgent2ConfiscationPayout = BigInteger.ZERO,
            onChainMatchingProposals = BigInteger.ZERO,
            makerReaction = BigInteger.ZERO,
            takerReaction = BigInteger.ZERO,
            onChainState = BigInteger.ZERO,
            hasMakerPaidOut = false,
            hasTakerPaidOut = false,
            totalWithoutSpentServiceFees = BigInteger.ZERO,
            role = DisputeRole.DISPUTE_AGENT_0,
        )
        swapAndDispute.takerCommunicationKey = takerCommunicationKey
        swapAndDispute.disputeAgent0InterfaceID = disputeAgent0KeyPair.interfaceId
        disputeTruthSource.swapAndDisputes[swapID] = swapAndDispute
        databaseService.storeSwapAndDispute(swapAndDispute = swapAndDispute.toDatabaseSwapAndDispute())

        val message = PublicKeyAnnouncementAsUserForDispute(
            id = swapID,
            chainID = swapAndDispute.chainID,
            publicKey = takerKeyPair.getPublicKey()
        )

        disputeService.handlePublicKeyAnnouncementAsUserForDispute(message = message)

        val takerPublicKeyInDatabase = keyManagerService.getPublicKey(swapAndDispute.takerInterfaceID)
        assert(takerKeyPair.interfaceId.contentEquals(takerPublicKeyInDatabase!!.interfaceId))
        assertEquals("TCKAnnouncement", p2pService.messageType)
        assertEquals(swapID.toString(), p2pService.id)
        assertEquals(swapAndDispute.chainID.toString(), p2pService.chainID)
        assert(takerCommunicationKey.keyBytes.contentEquals(Base64.getDecoder().decode(p2pService.key)))
        assert(takerKeyPair.interfaceId.contentEquals(p2pService.recipientPublicKey!!.interfaceId))
        assert(disputeAgent0KeyPair.interfaceId.contentEquals(p2pService.senderKeyPair!!.interfaceId))
        val swapAndDisputeInDatabase = databaseService.getSwapAndDispute(id = swapID.toString())
        assertEquals(1L, swapAndDisputeInDatabase!!.sentKeyToTaker)
        assert(swapAndDispute.sentKeyToTaker)
    }

    /**
     * Ensures that [DisputeService] properly handles [PublicKeyAnnouncementAsUserForDispute] messages sent by the maker
     * when they are the non-dispute-raising counterparty, when the user of this interface is the first dispute agent.
     */
    @Test
    fun testHandlePublicKeyAnnouncementAsUserForDispute_asFirstDisputeAgent_makerNonDisputeRaisingCounterparty() =
        runBlocking {
        val swapID = UUID.randomUUID()

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val disputeTruthSource = TestDisputeTruthSource()
        disputeService.setDisputeTruthSource(disputeTruthSource)

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val blockchainService = BlockchainService(
            exceptionHandler = TestBlockchainExceptionHandler(),
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = ""
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
        ).apply { accessToken.value = "" }
        class TestP2PService: P2PService(
            exceptionHandler = TestP2PExceptionHandler(),
            offerService = TestOfferMessageNotifiable(),
            swapService = TestSwapMessageNotifiable(),
            disputeService = disputeService,
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {

            var messageType: String? = null
            var id: String? = null
            var chainID: String? = null
            var key: String? = null
            var recipientPublicKey: PublicKey? = null
            var senderKeyPair: KeyPair? = null

            override suspend fun sendCommunicationKey(
                messageType: String,
                id: String,
                chainID: String,
                key: String,
                recipientPublicKey: PublicKey,
                senderKeyPair: KeyPair
            ) {
                this.messageType = messageType
                this.id = id
                this.chainID = chainID
                this.key = key
                this.recipientPublicKey = recipientPublicKey
                this.senderKeyPair = senderKeyPair
            }
        }
        val p2pService = TestP2PService()
        disputeService.setP2PService(p2pService)

        val makerKeyPair = KeyPair()
        val makerCommunicationKey = SymmetricKey()
        val disputeAgent0KeyPair = keyManagerService.generateKeyPair(storeResult=true)
        val swapAndDispute = SwapAndDispute(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "",
            makerInterfaceID = makerKeyPair.interfaceId,
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
            onChainDisputeRaiser = BigInteger.valueOf(2L), // Taker is dispute raiser
            chainID = BigInteger.valueOf(31337L),
            disputeRaisedBlockNumber = BigInteger.ZERO,
            disputeAgent0 = blockchainService.getAddress(),
            disputeAgent1 = "",
            disputeAgent2 = "",
            hasDisputeAgent0Proposed = false,
            disputeAgent0MakerPayout = BigInteger.ZERO,
            disputeAgent0TakerPayout = BigInteger.ZERO,
            disputeAgent0ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent1Proposed = false,
            disputeAgent1MakerPayout = BigInteger.ZERO,
            disputeAgent1TakerPayout = BigInteger.ZERO,
            disputeAgent1ConfiscationPayout = BigInteger.ZERO,
            hasDisputeAgent2Proposed = false,
            disputeAgent2MakerPayout = BigInteger.ZERO,
            disputeAgent2TakerPayout = BigInteger.ZERO,
            disputeAgent2ConfiscationPayout = BigInteger.ZERO,
            onChainMatchingProposals = BigInteger.ZERO,
            makerReaction = BigInteger.ZERO,
            takerReaction = BigInteger.ZERO,
            onChainState = BigInteger.ZERO,
            hasMakerPaidOut = false,
            hasTakerPaidOut = false,
            totalWithoutSpentServiceFees = BigInteger.ZERO,
            role = DisputeRole.DISPUTE_AGENT_0,
        )
        swapAndDispute.makerCommunicationKey = makerCommunicationKey
        swapAndDispute.disputeAgent0InterfaceID = disputeAgent0KeyPair.interfaceId
        disputeTruthSource.swapAndDisputes[swapID] = swapAndDispute
        databaseService.storeSwapAndDispute(swapAndDispute = swapAndDispute.toDatabaseSwapAndDispute())

        val message = PublicKeyAnnouncementAsUserForDispute(
            id = swapID,
            chainID = swapAndDispute.chainID,
            publicKey = makerKeyPair.getPublicKey()
        )

        disputeService.handlePublicKeyAnnouncementAsUserForDispute(message = message)

        val makerPublicKeyInDatabase = keyManagerService.getPublicKey(swapAndDispute.makerInterfaceID)
        assert(makerKeyPair.interfaceId.contentEquals(makerPublicKeyInDatabase!!.interfaceId))
        assertEquals("MCKAnnouncement", p2pService.messageType)
        assertEquals(swapID.toString(), p2pService.id)
        assertEquals(swapAndDispute.chainID.toString(), p2pService.chainID)
        assert(makerCommunicationKey.keyBytes.contentEquals(Base64.getDecoder().decode(p2pService.key)))
        assert(makerKeyPair.interfaceId.contentEquals(p2pService.recipientPublicKey!!.interfaceId))
        assert(disputeAgent0KeyPair.interfaceId.contentEquals(p2pService.senderKeyPair!!.interfaceId))
        val swapAndDisputeInDatabase = databaseService.getSwapAndDispute(id = swapID.toString())
        assertEquals(1L, swapAndDisputeInDatabase!!.sentKeyToMaker)
        assert(swapAndDispute.sentKeyToMaker)
    }

    /**
     * Ensures that [DisputeService.createRaiseDisputeTransaction], [BlockchainService.createRaiseDisputeTransaction],
     * [DisputeService.raiseDispute] and [BlockchainService.createRaiseDisputeTransaction] function properly.
     */
    @Test
    fun testRaiseDispute() = runBlocking {
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

        val w3 = CommutoWeb3j(HttpService(System.getenv("BLOCKCHAIN_NODE")))

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val exceptionHandler = TestBlockchainExceptionHandler()

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService  = keyManagerService,
        )

        val blockchainService = BlockchainService(
            exceptionHandler = exceptionHandler,
            offerService = TestOfferService(),
            swapService = TestSwapService(),
            disputeService = disputeService,
            web3 = w3,
            commutoSwapAddress = testingServerResponse.commutoSwapAddress
        )
        disputeService.setBlockchainService(newBlockchainService = blockchainService)

        val swap = Swap(
            isCreated = true,
            requiresFill = false,
            id = swapID,
            maker = "0x0000000000000000000000000000000000000000",
            makerInterfaceID = ByteArray(0),
            taker = "0x0000000000000000000000000000000000000000",
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
        val swapForDatabase = com.commuto.interfacedesktop.db.Swap(
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
            role = swap.role.asString,
            approveToFillState = swap.approvingToFillState.value.asString,
            approveToFillTransactionHash = null,
            approveToFillTransactionCreationTime = null,
            approveToFillTransactionCreationBlockNumber = null,
            fillingSwapState = swap.fillingSwapState.value.asString,
            fillingSwapTransactionHash = null,
            fillingSwapTransactionCreationTime = null,
            fillingSwapTransactionCreationBlockNumber = null,
            reportPaymentSentState = swap.reportingPaymentSentState.value.asString,
            reportPaymentSentTransactionHash = null,
            reportPaymentSentTransactionCreationTime = null,
            reportPaymentSentTransactionCreationBlockNumber = null,
            reportPaymentReceivedState = swap.reportingPaymentReceivedState.value.asString,
            reportPaymentReceivedTransactionHash = null,
            reportPaymentReceivedTransactionCreationTime = null,
            reportPaymentReceivedTransactionCreationBlockNumber = null,
            closeSwapState = swap.closingSwapState.value.asString,
            closeSwapTransactionHash = null,
            closeSwapTransactionCreationTime = null,
            closeSwapTransactionCreationBlockNumber = null,
            disputeState = swap.disputeState.value.asString,
            raisingDisputeState = swap.raisingDisputeState.value.asString,
            raisingDisputeTransactionHash = null,
            raisingDisputeTransactionCreationTime = null,
            raisingDisputeTransactionCreationBlockNumber = null,
        )
        databaseService.storeSwap(swapForDatabase)

        val raiseDisputeTransactionAndDisputeAgents = disputeService.createRaiseDisputeTransaction(
            swap = swap
        )

        disputeService.raiseDispute(
            swap = swap,
            disputeRaisingTransaction = raiseDisputeTransactionAndDisputeAgents.first,
            disputeAgents = raiseDisputeTransactionAndDisputeAgents.second
        )

        assertEquals(RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION, swap.raisingDisputeState.value)

        val swapInDatabase = databaseService.getSwap(encoder.encodeToString(swapID.asByteArray()))
        assertEquals(RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION.asString,
            swapInDatabase!!.raisingDisputeState)
        assertNotNull(swapInDatabase.raisingDisputeTransactionHash)
        assertNotNull(swapInDatabase.raisingDisputeTransactionCreationTime)
        assertNotNull(swapInDatabase.raisingDisputeTransactionCreationBlockNumber)

        val swapOnChain = blockchainService.getSwap(id = swapID)
        assertEquals(swapOnChain?.disputeRaiser, BigInteger.ONE)

        val disputeOnChain = blockchainService.getDisputeAsync(id = swapID).await()
        assertNotEquals(BigInteger.ZERO, disputeOnChain.disputeRaisedBlockNum)

    }

}