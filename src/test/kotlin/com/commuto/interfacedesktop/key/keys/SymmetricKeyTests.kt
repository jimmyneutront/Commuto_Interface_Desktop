package com.commuto.interfacedesktop.key.keys

import java.nio.charset.Charset
import kotlin.test.Test

class SymmetricKeyTests {

    @Test
    fun testSymmetricEncryption() {
        val key = SymmetricKey()
        val charset = Charset.forName("UTF-16")
        val originalData = "test".toByteArray(charset)
        val encryptedData = key.encrypt(originalData)
        assert(originalData.contentEquals(key.decrypt(encryptedData)))
    }

}