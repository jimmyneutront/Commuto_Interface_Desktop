package com.commuto.interfacedesktop.dbService

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.db.KeyPair
import com.commuto.interfacedesktop.db.PublicKey
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseServiceTest {
    private var driver = DatabaseDriverFactory()
    private var databaseService = DatabaseService(driver)

    /*
    @Test
    fun tempTest() {
        try {
            databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
            databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
        } catch (e: Exception) {
            print(e)
        }
    }
    */

    @BeforeTest
    fun testCreateTables() {
        databaseService.createTables()
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
    fun testDuplicateKeyPairProtection() = runBlocking {
        databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
        try {
            databaseService.storeKeyPair("interf_id", "pub_key", "priv_key")
        } catch (exception: IllegalStateException) {
            assert(exception.message!!.contains("Database query for key pair with interface id interf_id " +
                    "returned result"))
        }
    }

    @Test
    fun testDuplicatePublicKeyProtection() = runBlocking {
        databaseService.storePublicKey("interf_id", "pub_key")
        try {
            databaseService.storePublicKey("interf_id", "pub_key")
        } catch (exception: IllegalStateException) {
            assert(exception.message!!.contains("Database query for public key with interface id interf_id " +
                    "returned result"))
        }
    }

}