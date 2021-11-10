package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import java.security.KeyPair
import java.security.PublicKey
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
        val keyPairAndId: Pair<ByteArray, KeyPair> = kmService.generateKeyPair()
        val retrievedKpAndId: Pair<ByteArray, KeyPair>? = kmService.getKeyPair(keyPairAndId.first)
        assert(keyPairAndId.first.equals(retrievedKpAndId!!.first))
        assert(keyPairAndId.second.public.equals(retrievedKpAndId.second.public))
        assert(keyPairAndId.second.private.equals(retrievedKpAndId.second.private))
    }

    @Test
    fun testStoreAndRetrievePubKey() {
        val keyPairAndId: Pair<ByteArray, KeyPair> = kmService.generateKeyPair(storeResult = false)
        kmService.storePublicKey(keyPairAndId.second.public)
        val retrievedPkAndId: Pair<ByteArray,PublicKey>? = kmService.getPublicKey(keyPairAndId.first)
        assert(keyPairAndId.first.equals(retrievedPkAndId!!.first))
        assert(keyPairAndId.second.public.equals(retrievedPkAndId.second))
    }

}