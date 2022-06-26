package com.commuto.interfacedesktop.keymanager

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.keymanager.types.KeyPair
import com.commuto.interfacedesktop.keymanager.types.PublicKey
import com.commuto.interfacedesktop.keymanager.types.SymmetricKey
import com.commuto.interfacedesktop.keymanager.types.newSymmetricKey
import java.nio.charset.Charset
import java.util.Arrays
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KMTypesTest {

    private var driver = DatabaseDriverFactory()
    private var databaseService = DatabaseService(driver)
    private val kmService = KMService(databaseService)

    @BeforeTest
    fun testCreateTables() {
        databaseService.createTables()
    }

    @Test
    fun testSymmetricEncryption() {
        val key: SymmetricKey = newSymmetricKey()
        val charset = Charset.forName("UTF-16")
        val originalData = "test".toByteArray(charset)
        val encryptedData = key.encrypt(originalData)
        assert(Arrays.equals(originalData, key.decrypt(encryptedData)))
    }

    @Test
    fun testSignatureUtils() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val charset = Charset.forName("UTF-16")
        val signature = keyPair.sign("test".toByteArray(charset))
        assert(keyPair.verifySignature("test".toByteArray(charset), signature))
    }

    @Test
    fun testAsymmetricEncryptionWithKeyPair() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val charset = Charset.forName("UTF-16")
        val originalString = "test"
        val originalMessage = originalString.toByteArray(charset)
        val encryptedMessage = keyPair.encrypt(originalMessage)
        val decryptedMessage = keyPair.decrypt(encryptedMessage)
        val restoredString = String(decryptedMessage, charset)
        assert(originalString.equals(restoredString))
    }

    @Test
    fun testPubKeyPkcs1Operations() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val pubKeyPkcs1Bytes: ByteArray = keyPair.pubKeyToPkcs1Bytes()
        val restoredPubKey = PublicKey(pubKeyPkcs1Bytes)
        assert(keyPair.keyPair.public.equals(restoredPubKey.publicKey))
    }

    @Test
    fun testPrivKeyPkcs1Operations() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val pubKeyPkcs1Bytes: ByteArray = keyPair.pubKeyToPkcs1Bytes()
        val privKeyPkcs1Bytes: ByteArray = keyPair.privKeyToPkcs1Bytes()
        val restoredKeyPair = KeyPair(pubKeyPkcs1Bytes, privKeyPkcs1Bytes)
        assert(keyPair.keyPair.private.equals(restoredKeyPair.keyPair.private))
    }
}