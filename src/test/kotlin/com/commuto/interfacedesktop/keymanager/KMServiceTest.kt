package com.commuto.interfacedesktop.keymanager

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.keymanager.types.KeyPair
import com.commuto.interfacedesktop.keymanager.types.PublicKey
import java.util.*
import kotlin.test.Test
import kotlin.test.BeforeTest

internal class KMServiceTest {

    private var driver = DatabaseDriverFactory()
    private var dbService = DBService(driver)
    private val kmService = KMService(dbService)

    @BeforeTest
    fun testCreateTables() {
        dbService.createTables()
    }

    @Test
    fun testGenerateAndRetrieveKeyPair() {
        val keyPair: KeyPair = kmService.generateKeyPair()
        val retrievedKeyPair: KeyPair? = kmService.getKeyPair(keyPair.interfaceId)
        assert(Arrays.equals(keyPair.interfaceId, retrievedKeyPair!!.interfaceId))
        assert(keyPair.keyPair.public.equals(retrievedKeyPair.keyPair.public))
        assert(keyPair.keyPair.private.equals(retrievedKeyPair.keyPair.private))
    }

    @Test
    fun testStoreAndRetrievePubKey() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val publicKey = PublicKey(keyPair.keyPair.public)
        kmService.storePublicKey(publicKey)
        val retrievedPublicKey: PublicKey? = kmService.getPublicKey(publicKey.interfaceId)
        assert(Arrays.equals(publicKey.interfaceId, retrievedPublicKey!!.interfaceId))
        assert(publicKey.publicKey.equals(retrievedPublicKey.publicKey))
    }

}