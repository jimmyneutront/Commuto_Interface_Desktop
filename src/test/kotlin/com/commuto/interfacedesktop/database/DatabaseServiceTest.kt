package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Assert.assertNull
import java.util.*

class DatabaseServiceTest {
    private var driver = DatabaseDriverFactory()
    private var databaseService = DatabaseService(driver)

    @BeforeTest
    fun testCreateTables() {
        databaseService.createTables()
    }

    @Test
    fun testStoreAndGetAndDeleteOffer() = runBlocking {
        val offerToStore = Offer(
            "a_uuid",
            1L,
            0L,
            "maker_address",
            "interface_id",
            "stablecoin_address",
            "lower_bound_amount",
            "upper_bound_amount",
            "security_deposit_amount",
            "service_fee_rate",
            "direction",
            "some_version",
            "a_chain_id",
            0L,
            0L,
            "a_state_here",
        )
        databaseService.storeOffer(offerToStore)
        val anotherOfferToStore = Offer(
            "a_uuid",
            1L,
            0L,
            "another_maker_address",
            "another_interface_id",
            "another_stablecoin_address",
            "another_lower_bound_amount",
            "another_upper_bound_amount",
            "another_security_deposit_amount",
            "another_service_fee_rate",
            "opposite_direction",
            "some_other_version",
            "another_chain_id",
            0L,
            0L,
            "a_state_here",
        )
        // This should do nothing and not throw
        databaseService.storeOffer(anotherOfferToStore)
        // This should not throw since only one such Offer should exist in the database
        val returnedOffer = databaseService.getOffer("a_uuid")
        assertEquals(returnedOffer, offerToStore)
        databaseService.deleteOffers("a_uuid", "a_chain_id")
        val returnedOfferAfterDeletion = databaseService.getOffer("a_uuid")
        assertEquals(null, returnedOfferAfterDeletion)
    }

    @org.junit.Test
    fun testUpdateOfferHavePublicKey() = runBlocking {
        val offerToStore = Offer(
            "a_uuid",
            1L,
            0L,
            "maker_address",
            "interface_id",
            "stablecoin_address",
            "lower_bound_amount",
            "upper_bound_amount",
            "security_deposit_amount",
            "service_fee_rate",
            "direction",
            "some_version",
            "a_chain_id",
            0L,
            0L,
            "a_state_here",
        )
        databaseService.storeOffer(offerToStore)
        databaseService.updateOfferHavePublicKey("a_uuid", "a_chain_id", true)
        val returnedOffer = databaseService.getOffer("a_uuid")
        Assert.assertEquals(returnedOffer!!.havePublicKey, 1L)
    }

    /**
     * Ensures that code to update a persistently stored [Offer.state] property works properly.
     */
    @org.junit.Test
    fun testUpdateOfferState() = runBlocking {
        val offerToStore = Offer(
            "a_uuid",
            1L,
            0L,
            "maker_address",
            "interface_id",
            "stablecoin_address",
            "lower_bound_amount",
            "upper_bound_amount",
            "security_deposit_amount",
            "service_fee_rate",
            "direction",
            "some_version",
            "a_chain_id",
            0L,
            0L,
            "a_state_here"
        )
        databaseService.storeOffer(offerToStore)
        databaseService.updateOfferState("a_uuid", "a_chain_id", "a_new_state_here")
        val returnedOffer = databaseService.getOffer("a_uuid")
        assertEquals("a_new_state_here", returnedOffer!!.state)
    }

    /**
     * Ensures code to store, get and delete settlement methods works properly.
     */
    @Test
    fun testStoreAndGetAndDeleteSettlementMethods() = runBlocking {
        val offerId = "an_offer_id"
        val chainID = "a_chain_id"
        val differentChainID = "different_chain_id"
        val settlementMethods = listOf(
            Pair("settlement_method_zero", "private_data_zero"),
            Pair("settlement_method_one", "private_data_one"),
            Pair("settlement_method_two", "private_data_two"),
        )
        databaseService.storeOfferSettlementMethods(offerId, chainID, settlementMethods)
        databaseService.storeOfferSettlementMethods(offerId, differentChainID, settlementMethods)
        val receivedSettlementMethods = databaseService.getOfferSettlementMethods(offerId, chainID)!!
        assertEquals(3, receivedSettlementMethods.size)
        assertEquals("settlement_method_zero", receivedSettlementMethods[0].first)
        assertEquals("private_data_zero", receivedSettlementMethods[0].second)
        assertEquals("settlement_method_one", receivedSettlementMethods[1].first)
        assertEquals("private_data_one", receivedSettlementMethods[1].second)
        assertEquals("settlement_method_two", receivedSettlementMethods[2].first)
        assertEquals("private_data_two", receivedSettlementMethods[2].second)
        val newSettlementMethods = listOf(
            Pair("settlement_method_three", "private_data_three"),
            Pair("settlement_method_four", "private_data_four"),
            Pair("settlement_method_five", "private_data_five"),
        )
        databaseService.storeOfferSettlementMethods(offerId, chainID, newSettlementMethods)
        val newReturnedSettlementMethods = databaseService.getOfferSettlementMethods(offerId, chainID)!!
        assertEquals(3, newReturnedSettlementMethods.size)
        assertEquals("settlement_method_three", newReturnedSettlementMethods[0].first)
        assertEquals("private_data_three", newReturnedSettlementMethods[0].second)
        assertEquals("settlement_method_four", newReturnedSettlementMethods[1].first)
        assertEquals("private_data_four", newReturnedSettlementMethods[1].second)
        assertEquals("settlement_method_five", newReturnedSettlementMethods[2].first)
        assertEquals("private_data_five", newReturnedSettlementMethods[2].second)
        databaseService.deleteOfferSettlementMethods(offerId, chainID)
        val returnedSettlementMethodsAfterDeletion = databaseService.getOfferSettlementMethods(offerId, chainID)
        Assert.assertEquals(null, returnedSettlementMethodsAfterDeletion)
        val differentSettlementMethods = databaseService.getOfferSettlementMethods(offerId, differentChainID)!!
        assertEquals(3, differentSettlementMethods.size)
        assertEquals("settlement_method_zero", differentSettlementMethods[0].first)
        assertEquals("private_data_zero", differentSettlementMethods[0].second)
        assertEquals("settlement_method_one", differentSettlementMethods[1].first)
        assertEquals("private_data_one", differentSettlementMethods[1].second)
        assertEquals("settlement_method_two", differentSettlementMethods[2].first)
        assertEquals("private_data_two", differentSettlementMethods[2].second)
    }

    /**
     * Ensures code to store, get and delete pending settlement methods works properly.
     */
    @Test
    fun testStoreAndGetAndDeletePendingSettlementMethods() = runBlocking {
        val offerID = "an_offer_id"
        val chainID = "a_chain_id"
        val differentChainID = "different_chain_id"
        val settlementMethods = listOf(
            Pair("settlement_method_zero", "private_data_zero"),
            Pair("settlement_method_one", "private_data_one"),
            Pair("settlement_method_two", "private_data_two"),
        )
        databaseService.storePendingOfferSettlementMethods(offerID, chainID, settlementMethods)
        databaseService.storePendingOfferSettlementMethods(offerID, differentChainID, settlementMethods)
        val receivedSettlementMethods = databaseService.getPendingOfferSettlementMethods(offerID, chainID)!!
        assertEquals(3, receivedSettlementMethods.size)
        assertEquals("settlement_method_zero", receivedSettlementMethods[0].first)
        assertEquals("private_data_zero", receivedSettlementMethods[0].second)
        assertEquals("settlement_method_one", receivedSettlementMethods[1].first)
        assertEquals("private_data_one", receivedSettlementMethods[1].second)
        assertEquals("settlement_method_two", receivedSettlementMethods[2].first)
        assertEquals("private_data_two", receivedSettlementMethods[2].second)
        val newSettlementMethods = listOf(
            Pair("settlement_method_three", "private_data_three"),
            Pair("settlement_method_four", "private_data_four"),
            Pair("settlement_method_five", "private_data_five"),
        )
        databaseService.storePendingOfferSettlementMethods(offerID, chainID, newSettlementMethods)
        val newReturnedSettlementMethods = databaseService.getPendingOfferSettlementMethods(offerID, chainID)!!
        assertEquals(3, newReturnedSettlementMethods.size)
        assertEquals("settlement_method_three", newReturnedSettlementMethods[0].first)
        assertEquals("private_data_three", newReturnedSettlementMethods[0].second)
        assertEquals("settlement_method_four", newReturnedSettlementMethods[1].first)
        assertEquals("private_data_four", newReturnedSettlementMethods[1].second)
        assertEquals("settlement_method_five", newReturnedSettlementMethods[2].first)
        assertEquals("private_data_five", newReturnedSettlementMethods[2].second)
        databaseService.deletePendingOfferSettlementMethods(offerID, chainID)
        val returnedSettlementMethodsAfterDeletion = databaseService.getPendingOfferSettlementMethods(offerID, chainID)
        assertEquals(null, returnedSettlementMethodsAfterDeletion)
        val differentSettlementMethods = databaseService.getPendingOfferSettlementMethods(offerID, differentChainID)!!
        assertEquals(3, differentSettlementMethods.size)
        assertEquals("settlement_method_zero", differentSettlementMethods[0].first)
        assertEquals("private_data_zero", differentSettlementMethods[0].second)
        assertEquals("settlement_method_one", differentSettlementMethods[1].first)
        assertEquals("private_data_one", differentSettlementMethods[1].second)
        assertEquals("settlement_method_two", differentSettlementMethods[2].first)
        assertEquals("private_data_two", differentSettlementMethods[2].second)
    }

    /**
     * Ensures the code to store, get and delete a [Swap] from persistent storage works properly.
     */
    @Test
    fun testStoreAndGetAndDeleteSwap() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 0L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            protocolVersion = "some_version",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = null,
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = null,
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here",
        )
        databaseService.storeSwap(swapToStore)
        val anotherSwapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 0L,
            maker = "another_maker_address",
            makerInterfaceID = "another_maker_interface_id",
            taker = "another_taker_address",
            takerInterfaceID = "another_taker_interface_id",
            stablecoin = "another_stablecoin_address",
            amountLowerBound = "another_lower_bound_amount",
            amountUpperBound = "another_upper_bound_amount",
            securityDepositAmount = "another_security_deposit_amount",
            takenSwapAmount = "another_taken_swap_amount",
            serviceFeeAmount = "another_service_fee_amount",
            serviceFeeRate = "another_service_fee_rate",
            onChainDirection = "another_direction",
            settlementMethod = "another_settlement_method",
            protocolVersion = "another_some_version",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = null,
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = null,
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "another_dispute_raiser",
            chainID = "another_chain_id",
            state = "another_state_here",
            role = "another_role_here",
        )
        // This should do nothing and not throw
        databaseService.storeSwap(anotherSwapToStore)
        // This should not throw since only one such Swap should exist in the database
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(swapToStore, returnedSwap)
        databaseService.deleteSwaps("a_uuid", "a_chain_id")
        assertNull(databaseService.getSwap("a_uuid"))
    }

    /**
     * Ensures that code to update a persistently stored [Swap.requiresFill] property works properly.
     */
    @Test
    fun testUpdateSwapRequiresFill() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapRequiresFill("a_uuid", "a_chain_id", false)
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(0L, returnedSwap!!.requiresFill)
    }

    /**
     * Ensures that code to update a persistently stored swap's maker private settlement method data related properties
     * works properly.
     */
    @Test
    fun testUpdateSwapMakerPrivateSettlementMethodData() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_init_vector",
            protocolVersion = "some_version",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapMakerPrivateSettlementMethodData(
            swapID = "a_uuid",
            chainID = "a_chain_id",
            data = "new_maker_private_data"
        )
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals("new_maker_private_data", returnedSwap!!.makerPrivateData)
    }

    /**
     * Ensures that code to update a persistently stored swap's taker private settlement method data related properties
     * works properly.
     */
    @Test
    fun testUpdateSwapTakerPrivateSettlementMethodData() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_vector",
            protocolVersion = "some_version",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapTakerPrivateSettlementMethodData(
            swapID = "a_uuid",
            chainID = "a_chain_id",
            data = "new_taker_private_data"
        )
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals("new_taker_private_data", returnedSwap!!.takerPrivateData)
    }

    /**
     * Ensures that code to update a persistently stored [Swap.isPaymentSent] property works properly.
     */
    @Test
    fun testUpdateSwapIsPaymentSent() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapIsPaymentSent("a_uuid", "a_chain_id", true)
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(1L, returnedSwap!!.isPaymentSent)
    }

    /**
     * Ensures that code to update a persistently stored [Swap.isPaymentReceived] property works properly.
     */
    @Test
    fun testUpdateSwapIsPaymentReceived() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 1L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapIsPaymentReceived("a_uuid", "a_chain_id", true)
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(1L, returnedSwap!!.isPaymentReceived)
    }

    /**
     * Ensures that code to update a persistently stored [Swap.hasBuyerClosed] property works properly.
     */
    @Test
    fun testUpdateSwapHasBuyerClosed() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 1L,
            isPaymentReceived = 1L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapHasBuyerClosed("a_uuid", "a_chain_id", true)
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(1L, returnedSwap!!.hasBuyerClosed)
    }

    /**
     * Ensures that code to update a persistently stored [Swap.hasSellerClosed] property works properly.
     */
    @Test
    fun testUpdateSwapHasSellerClosed() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 1L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 1L,
            isPaymentReceived = 1L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapHasSellerClosed("a_uuid", "a_chain_id", true)
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals(1L, returnedSwap!!.hasSellerClosed)
    }

    /**
     * Ensures that code to update a persistently stored [Swap.state] property works properly.
     */
    @Test
    fun testUpdateSwapState() = runBlocking {
        val swapToStore = Swap(
            id = "a_uuid",
            isCreated = 1L,
            requiresFill = 0L,
            maker = "maker_address",
            makerInterfaceID = "maker_interface_id",
            taker = "taker_address",
            takerInterfaceID = "taker_interface_id",
            stablecoin = "stablecoin_address",
            amountLowerBound = "lower_bound_amount",
            amountUpperBound = "upper_bound_amount",
            securityDepositAmount = "security_deposit_amount",
            takenSwapAmount = "taken_swap_amount",
            serviceFeeAmount = "service_fee_amount",
            serviceFeeRate = "service_fee_rate",
            onChainDirection = "direction",
            settlementMethod = "settlement_method",
            makerPrivateData = "maker_private_data",
            makerPrivateDataInitializationVector = "maker_init_vector",
            takerPrivateData = "taker_private_data",
            takerPrivateDataInitializationVector = "taker_private_data",
            protocolVersion = "some_version",
            isPaymentSent = 0L,
            isPaymentReceived = 0L,
            hasBuyerClosed = 0L,
            hasSellerClosed = 0L,
            disputeRaiser = "dispute_raiser",
            chainID = "a_chain_id",
            state = "a_state_here",
            role = "a_role_here"
        )
        databaseService.storeSwap(swapToStore)
        databaseService.updateSwapState("a_uuid", "a_chain_id", "a_new_state_here")
        val returnedSwap = databaseService.getSwap("a_uuid")
        assertEquals("a_new_state_here", returnedSwap!!.state)
    }

    /**
     * Ensures code to store and get settlement methods works properly.
     */
    @Test
    fun testStoreAndGetAndUpdateAndDeleteUserSettlementMethods() = runBlocking {
        val settlementMethodID = UUID.randomUUID()
        val settlementMethod = "settlement_method"
        val privateData = "some_private_data"

        databaseService.storeUserSettlementMethod(
            id = settlementMethodID.toString(),
            settlementMethod = settlementMethod,
            privateData = privateData
        )

        val returnedSettlementMethod = databaseService.getUserSettlementMethod(id = settlementMethodID.toString())

        assertEquals(settlementMethod, returnedSettlementMethod?.first)
        assertEquals(privateData, returnedSettlementMethod?.second)

        databaseService.updateUserSettlementMethod(
            id = settlementMethodID.toString(),
            privateData = "updated_private_data"
        )

        val updatedReturnedSettlementMethod = databaseService.getUserSettlementMethod(
            id = settlementMethodID.toString()
        )

        assertEquals(settlementMethod, updatedReturnedSettlementMethod?.first)
        assertEquals("updated_private_data", updatedReturnedSettlementMethod?.second)

        databaseService.deleteUserSettlementMethod(id = settlementMethodID.toString())

        val returnedSettlementMethodAfterDeletion = databaseService.getUserSettlementMethod(
            id = settlementMethodID.toString()
        )

        assertNull(returnedSettlementMethodAfterDeletion)

    }

    @Test
    fun testStoreAndGetKeyPair() = runBlocking {
        databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
        val expectedKeyPair = KeyPair("interf_id", "pub_key", "priv_key")
        val keyPair: KeyPair? = databaseService.getKeyPair("interf_id")
        assertEquals(keyPair!!, expectedKeyPair)
    }

    @Test
    fun testStoreAndGetPublicKey() = runBlocking {
        databaseService.storePublicKey("interf_id", "pub_key")
        val expectedPublicKey = PublicKey("interf_id", "pub_key")
        val publicKey: PublicKey? = databaseService.getPublicKey("interf_id")
        assertEquals(publicKey!!, expectedPublicKey)
    }

    @Test
    fun testDuplicateKeyPairProtection(): Unit = runBlocking {
        databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
        // This should do nothing and not throw
        databaseService.storeKeyPair("interf_id", "another_pub_key", "another_priv_key")
        // This should not throw, since only one such key pair should exist in the database
        val keyPair = databaseService.getKeyPair("interf_id")
        assertEquals(keyPair!!.publicKey, "pub_key")
        assertEquals(keyPair.privateKey, "priv_key")
    }

    @Test
    fun testDuplicatePublicKeyProtection() = runBlocking {
        databaseService.storePublicKey("interf_id", "pub_key")
        // This should do nothing and not throw
        databaseService.storePublicKey("interf_id", "another_pub_key")
        // This should not throw, since only one such key pair should exist in the database
        val pubKey = databaseService.getPublicKey("interf_id")
        assertEquals(pubKey!!.publicKey, "pub_key")
    }

}