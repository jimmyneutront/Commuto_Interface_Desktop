package com.commuto.interfacedesktop.key

import com.commuto.interfacedesktop.key.keys.SymmetricKey
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

/**
 * Derives a 32 byte [SymmetricKey] for AES encryption/decryption from a given password [String] and salt [ByteArray]
 * using HKDF with SHA256.
 *
 * @param password The password [String] from which the [SymmetricKey] will be derived.
 * @param salt The salt to use during [SymmetricKey] derivation.
 *
 * @property key The 32 byte [SymmetricKey] derived from the given password `String` and salt `ByteArray`.
 */
class DatabaseKeyDeriver(password: String, salt: ByteArray) {

    val key: SymmetricKey

    init {
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(
            HKDFParameters(
                password.toByteArray(),
                salt,
                null,
            )
        )
        val keyByteArray = ByteArray(32)
        generator.generateBytes(keyByteArray, 0, 32)
        key = SymmetricKey(key = keyByteArray)
    }

}