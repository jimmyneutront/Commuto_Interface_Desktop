package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KMServiceUtilsTest {

    private var driver = DatabaseDriverFactory()
    private var dbService = DBService(driver)
    private val kmService = KMService(dbService)

    @BeforeTest
    fun testCreateTables() {
        dbService.createTables()
    }

    @Test
    fun testPubKeyPkcs1Operations() {
        val keyPairAndId: Pair<ByteArray, KeyPair> = kmService.generateKeyPair(storeResult = false)
        val pubKeyPkcs1Bytes: ByteArray = kmService.pubKeyToPkcs1Bytes(keyPairAndId.second.public)
        val restoredPubKey: PublicKey = kmService.pubKeyFromPkcs1Bytes(pubKeyPkcs1Bytes)
        assert(keyPairAndId.second.public.equals(restoredPubKey))
    }

    @Test
    fun testPrivKeyPkcs1Operations() {
        val keyPairAndId: Pair<ByteArray, KeyPair> = kmService.generateKeyPair(storeResult = false)
        val privKeyPkcs1Bytes: ByteArray = kmService.privKeyToPkcs1Bytes(keyPairAndId.second.private)
        val restoredPrivKey: PrivateKey = kmService.privKeyFromPkcs1Bytes(privKeyPkcs1Bytes)
        assert(keyPairAndId.second.private.equals(restoredPrivKey))
    }
}