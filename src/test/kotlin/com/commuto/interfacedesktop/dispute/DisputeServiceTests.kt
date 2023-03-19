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
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.TestOfferService
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.p2p.TestOfferMessageNotifiable
import com.commuto.interfacedesktop.p2p.TestP2PExceptionHandler
import com.commuto.interfacedesktop.p2p.TestSwapMessageNotifiable
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
        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()
        val keyManagerService = KeyManagerService(databaseService)

        val disputeService = DisputeService(
            databaseService = databaseService,
            keyManagerService = keyManagerService
        )
        val swapTruthSource = TestSwapTruthSource()
        disputeService.setSwapTruthSource(swapTruthSource)

        val swapID = UUID.randomUUID()

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
            mxClient = mxClient,
            keyManagerService = keyManagerService,
        ) {
            var userKeyPair: KeyPair? = null
            override suspend fun announcePublicKeyAsUserForDispute(
                keyPair: KeyPair
            ) {
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

        assert(swap.makerInterfaceID.contentEquals(p2pService.userKeyPair?.interfaceId))
        assertEquals(DisputeState.SENT_PKA, swap.disputeState.value)
        assertEquals(RaisingDisputeState.COMPLETED, swap.raisingDisputeState.value)
        val swapInDatabase = databaseService.getSwap(swapForDatabase.id)
        assertEquals(DisputeState.SENT_PKA.asString, swapInDatabase?.disputeState)
        assertEquals(RaisingDisputeState.COMPLETED.asString, swapInDatabase?.raisingDisputeState)
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