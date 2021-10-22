package com.commuto.interfacedesktop.dbService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.db.KeyPair
import com.commuto.interfacedesktop.db.PublicKey
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DBServiceTest {
    private var driver = DatabaseDriverFactory()
    private var dbService = DBService(driver)

    @BeforeTest
    fun testCreateTables() {
        dbService.createTables()
    }

    @Test
    fun testStoreAndGetKeyPair() {
        dbService.storeKeyPair("interf_id", "pub_key", "priv_key")
        val expectedKeyPair = KeyPair("interf_id", "pub_key", "priv_key")
        val keyPairs: List<KeyPair> = dbService.getKeyPair("interf_id")
        assertTrue(keyPairs.size == 1)
        assertEquals(keyPairs[0], expectedKeyPair)
    }

    @Test
    fun testStoreAndGetPublicKey() {
        dbService.storePublicKey("interf_id", "pub_key")
        val expectedPublicKey = PublicKey("interf_id", "pub_key")
        val publicKeys: List<PublicKey> = dbService.getPublicKey("interf_id")
        assertTrue(publicKeys.size == 1)
        assertEquals(publicKeys[0], expectedPublicKey)
    }
}