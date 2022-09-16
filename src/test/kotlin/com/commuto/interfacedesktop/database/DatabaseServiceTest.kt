package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Assert.assertNull

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

    @Test
    fun testStoreAndGetAndDeleteSettlementMethods() = runBlocking {
        val offerId = "an_offer_id"
        val chainID = "a_chain_id"
        val differentChainID = "different_chain_id"
        val settlementMethods = listOf(
            "settlement_method_zero",
            "settlement_method_one",
            "settlement_method_two",
        )
        databaseService.storeSettlementMethods(offerId, chainID, settlementMethods)
        databaseService.storeSettlementMethods(offerId, differentChainID, settlementMethods)
        val receivedSettlementMethods = databaseService.getSettlementMethods(offerId, chainID)!!
        Assert.assertEquals(receivedSettlementMethods.size, 3)
        Assert.assertEquals(receivedSettlementMethods[0], "settlement_method_zero")
        Assert.assertEquals(receivedSettlementMethods[1], "settlement_method_one")
        Assert.assertEquals(receivedSettlementMethods[2], "settlement_method_two")
        val newSettlementMethods = listOf(
            "settlement_method_three",
            "settlement_method_four",
            "settlement_method_five",
        )
        databaseService.storeSettlementMethods(offerId, chainID, newSettlementMethods)
        val newReturnedSettlementMethods = databaseService.getSettlementMethods(offerId, chainID)!!
        Assert.assertEquals(newReturnedSettlementMethods.size, 3)
        Assert.assertEquals(newReturnedSettlementMethods[0], "settlement_method_three")
        Assert.assertEquals(newReturnedSettlementMethods[1], "settlement_method_four")
        Assert.assertEquals(newReturnedSettlementMethods[2], "settlement_method_five")
        databaseService.deleteSettlementMethods(offerId, chainID)
        val returnedSettlementMethodsAfterDeletion = databaseService.getSettlementMethods(offerId, chainID)
        Assert.assertEquals(null, returnedSettlementMethodsAfterDeletion)
        val differentSettlementMethods = databaseService.getSettlementMethods(offerId, differentChainID)!!
        Assert.assertEquals(differentSettlementMethods.size, 3)
        Assert.assertEquals(differentSettlementMethods[0], "settlement_method_zero")
        Assert.assertEquals(differentSettlementMethods[1], "settlement_method_one")
        Assert.assertEquals(differentSettlementMethods[2], "settlement_method_two")
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