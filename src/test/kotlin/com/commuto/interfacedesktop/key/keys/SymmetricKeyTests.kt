package com.commuto.interfacedesktop.key.keys

import java.nio.charset.Charset
import java.util.*
import kotlin.test.Test

class SymmetricKeyTests {

    @Test
    fun testSymmetricEncryption() {
        val key: SymmetricKey = newSymmetricKey()
        val charset = Charset.forName("UTF-16")
        val originalData = "test".toByteArray(charset)
        val encryptedData = key.encrypt(originalData)
        assert(Arrays.equals(originalData, key.decrypt(encryptedData)))
    }

}