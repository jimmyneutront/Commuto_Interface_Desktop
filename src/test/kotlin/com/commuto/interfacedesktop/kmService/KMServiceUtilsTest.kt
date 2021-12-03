package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.kmTypes.KeyPair
import java.nio.charset.Charset
import java.security.PrivateKey
import java.security.PublicKey as JavaSecPublicKey
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
        val restoredPubKey: JavaSecPublicKey = kmService.pubKeyFromPkcs1Bytes(pubKeyPkcs1Bytes)
        assert(keyPair.keyPair.public.equals(restoredPubKey))
    }

    @Test
    fun testPrivKeyPkcs1Operations() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val privKeyPkcs1Bytes: ByteArray = keyPair.privKeyToPkcs1Bytes()
        val restoredPrivKey: PrivateKey = kmService.privKeyFromPkcs1Bytes(privKeyPkcs1Bytes)
        assert(keyPair.keyPair.private.equals(restoredPrivKey))
    }
}