package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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