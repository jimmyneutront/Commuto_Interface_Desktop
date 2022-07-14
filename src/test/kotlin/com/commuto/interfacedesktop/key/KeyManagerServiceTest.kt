package com.commuto.interfacedesktop.key

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.key.keys.PublicKey
import kotlin.test.Test
import kotlin.test.BeforeTest

internal class KeyManagerServiceTest {

    private var driver = DatabaseDriverFactory()
    private var databaseService = DatabaseService(driver)
    private val keyManagerService = KeyManagerService(databaseService)

    @BeforeTest
    fun testCreateTables() {
        databaseService.createTables()
    }

    @Test
    fun testGenerateAndRetrieveKeyPair() {
        val keyPair: KeyPair = keyManagerService.generateKeyPair()
        val retrievedKeyPair: KeyPair? = keyManagerService.getKeyPair(keyPair.interfaceId)
        assert(keyPair.interfaceId.contentEquals(retrievedKeyPair!!.interfaceId))
        assert(keyPair.keyPair.public.equals(retrievedKeyPair.keyPair.public))
        assert(keyPair.keyPair.private.equals(retrievedKeyPair.keyPair.private))
    }

    @Test
    fun testStoreAndRetrievePubKey() {
        val keyPair: KeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val publicKey = PublicKey(keyPair.keyPair.public)
        keyManagerService.storePublicKey(publicKey)
        val retrievedPublicKey: PublicKey? = keyManagerService.getPublicKey(publicKey.interfaceId)
        assert(publicKey.interfaceId.contentEquals(retrievedPublicKey!!.interfaceId))
        assert(publicKey.publicKey == retrievedPublicKey.publicKey)
    }

}