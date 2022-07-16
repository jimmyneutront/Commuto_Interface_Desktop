package com.commuto.interfacedesktop.key.keys

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.key.KeyManagerService
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import kotlin.test.BeforeTest
import kotlin.test.Test

class KeyPairTests {

    private var driver = DatabaseDriverFactory()
    private var databaseService = DatabaseService(driver)
    private val keyManagerService = KeyManagerService(databaseService)

    @BeforeTest
    fun testCreateTables() {
        databaseService.createTables()
    }

    @Test
    fun testSignatureUtils() = runBlocking {
        val keyPair: KeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val charset = Charset.forName("UTF-16")
        val signature = keyPair.sign("test".toByteArray(charset))
        assert(keyPair.verifySignature("test".toByteArray(charset), signature))
    }

    @Test
    fun testAsymmetricEncryptionWithKeyPair() = runBlocking {
        val keyPair: KeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val charset = Charset.forName("UTF-16")
        val originalString = "test"
        val originalMessage = originalString.toByteArray(charset)
        val encryptedMessage = keyPair.encrypt(originalMessage)
        val decryptedMessage = keyPair.decrypt(encryptedMessage)
        val restoredString = String(decryptedMessage, charset)
        assert(originalString.equals(restoredString))
    }

    @Test
    fun testPubKeyPkcs1Operations() = runBlocking {
        val keyPair: KeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val pubKeyPkcs1Bytes: ByteArray = keyPair.pubKeyToPkcs1Bytes()
        val restoredPubKey = PublicKey(pubKeyPkcs1Bytes)
        assert(keyPair.keyPair.public.equals(restoredPubKey.publicKey))
    }

    @Test
    fun testPrivKeyPkcs1Operations() = runBlocking {
        val keyPair: KeyPair = keyManagerService.generateKeyPair(storeResult = false)
        val pubKeyPkcs1Bytes: ByteArray = keyPair.pubKeyToPkcs1Bytes()
        val privKeyPkcs1Bytes: ByteArray = keyPair.privKeyToPkcs1Bytes()
        val restoredKeyPair = KeyPair(pubKeyPkcs1Bytes, privKeyPkcs1Bytes)
        assert(keyPair.keyPair.private.equals(restoredKeyPair.keyPair.private))
    }
}