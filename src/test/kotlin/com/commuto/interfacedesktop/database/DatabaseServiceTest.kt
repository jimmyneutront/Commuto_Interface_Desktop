package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.KeyPair
import com.commuto.interfacedesktop.db.Offer
import com.commuto.interfacedesktop.db.OfferOpenedEvent
import com.commuto.interfacedesktop.db.PublicKey
import kotlinx.coroutines.runBlocking
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
    fun testStoreAndGetOffer() = runBlocking {
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
            "a_price",
            "some_version",
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
            "a_different_price",
            "some_other_version",
        )
        databaseService.storeOffer(anotherOfferToStore)
        val returnedOffer = databaseService.getOffer("a_uuid")
        assertEquals(returnedOffer, offerToStore)
    }

    @Test
    fun testStoreAndGetSettlementMethods() = runBlocking {
        val offerId = "an_offer_id"
        val settlementMethods = listOf(
            "settlement_method_zero",
            "settlement_method_one",
            "settlement_method_two",
        )
        databaseService.storeSettlementMethods(offerId, settlementMethods)
        val receivedSettlementMethods = databaseService.getSettlementMethods(offerId)!!
        assertEquals(receivedSettlementMethods[0], "settlement_method_zero")
        assertEquals(receivedSettlementMethods[1], "settlement_method_one")
        assertEquals(receivedSettlementMethods[2], "settlement_method_two")
    }

    @Test
    fun testStoreAndGetAndDeleteOfferOpenedEvent() = runBlocking {
        databaseService.storeOfferOpenedEvent("offer_id", "interf_id")
        // This should do nothing and not throw
        databaseService.storeOfferOpenedEvent("offer_id", "interf_id")
        val expectedOfferOpenedEvent = OfferOpenedEvent("offer_id", "interf_id")
        // This should not throw since only one such OfferOpened event should exist in the database
        val offerOpenedEvent = databaseService.getOfferOpenedEvent("offer_id")
        assertEquals(expectedOfferOpenedEvent, offerOpenedEvent)
        databaseService.deleteOfferOpenedEvents("offer_id")
        val offerOpenedEventAfterDeletion = databaseService.getOfferOpenedEvent("offer_id")
        assertEquals(offerOpenedEventAfterDeletion, null)
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