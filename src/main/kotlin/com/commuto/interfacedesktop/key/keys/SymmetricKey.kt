package com.commuto.interfacedesktop.key.keys

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// TODO: Don't use CBC
/**
 * This is an AES-256 key, with functions for encryption and decryption.
 *
 * @property key: The AES-256 key as a [SecretKey].
 * @property keyBytes: The AES-256 key as a [ByteArray]
 */
class SymmetricKey {
    val key: SecretKey
    val keyBytes: ByteArray

    /**
     * Creates a new [SymmetricKey] given an AES-256 [SecretKey].
     */
    constructor (key: SecretKey) {
        this.key = key
        this.keyBytes = key.encoded
    }

    /**
     * Creates a new [SymmetricKey] given an AES-256 key as a [ByteArray].
     */
    constructor(key: ByteArray) {
        this.keyBytes = key
        this.key = SecretKeySpec(key, "AES")
    }

    /**
     * Creates a new initialization vector and then performs AES encryption on the given bytes with this [SymmetricKey]
     * using CBC with PKCS#5 padding.
     *
     * @param data The bytes to be encrypted, as a [ByteArray].
     *
     * @returns [SymmetricallyEncryptedData], containing a new initialization vector and [data] encrypted with this
     * [SymmetricKey] and the new initialization vector.
     */
    fun encrypt(data: ByteArray): SymmetricallyEncryptedData {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, this.key)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data)
        return SymmetricallyEncryptedData(encryptedBytes, iv)
    }

    /**
     * Decrypts [SymmetricallyEncryptedData] that has been AES encrypted with this [SymmetricKey] using CBC with PKCS#5
     * padding.
     *
     * @param data The [SymmetricallyEncryptedData] to be decrypted, which contains the cipher data to be decrypted and
     * the initialization vector used to decrypt the cipher data.
     *
     * @returns The cipher data in the passed [SymmetricallyEncryptedData] decrypted with the initialization vector in
     * the passed [SymmetricallyEncryptedData] and this [SymmetricKey], as a [ByteArray].
     */
    fun decrypt(data: SymmetricallyEncryptedData): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, this.key, IvParameterSpec(data.initializationVector))
        return cipher.doFinal(data.encryptedData)
    }

}

// TODO: Move this to its own file
/**
 * Contains data encrypted with a [SymmetricKey] and the initialization vector used to encrypt it.
 *
 * @property encryptedData The encrypted data as a [ByteArray].
 * @property initializationVector The initialization vector used to encrypt this [SymmetricallyEncryptedData], as a
 * [ByteArray].
 */
class SymmetricallyEncryptedData(data: ByteArray, iv: ByteArray) {
    val encryptedData = data
    val initializationVector = iv
}

// TODO: make this a no-argument constructor of SymmetricKey
/**
 * Creates a new [SymmetricKey] wrapped around a new AES-256 [SecretKey].
 *
 * @return A new [SymmetricKey].
 */
fun newSymmetricKey(): SymmetricKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)
    return SymmetricKey(keyGenerator.generateKey())
}