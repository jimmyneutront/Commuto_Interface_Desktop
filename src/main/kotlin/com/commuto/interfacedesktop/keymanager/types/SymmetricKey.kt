package com.commuto.interfacedesktop.keymanager.types

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The SymmetricKey class is a wrapper around an AES-256 SecretKey, with methods for encrypting and decrypting data
 * using the key.
 *
 * @property key: the SecretKey object around which this class is wrapped.
 * @property keyBytes: a ByteArray representation of the wrapped SecretKey.
 */
class SymmetricKey {
    val key: SecretKey
    val keyBytes: ByteArray

    constructor (key: SecretKey) {
        this.key = key
        this.keyBytes = key.encoded
    }

    constructor(key: ByteArray) {
        this.keyBytes = key
        this.key = SecretKeySpec(key, "AES")
    }

    /**
     * Encrypt data with this key using CBC with PKCS5 padding.
     *
     * @param data: the data to be encrypted
     * @returns SymmetricallyEncryptedData: the encrypted data along with the initialization vector used.
     */
    fun encrypt(data: ByteArray): SymmetricallyEncryptedData {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, this.key)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data)
        return SymmetricallyEncryptedData(encryptedBytes, iv)
    }

    /**
     * Decrypt data encrypted using CBC with PKCS5 padding with this key.
     *
     * @param data: the SymmetricallyEncryptedData object containing both the encrypted data and the initialization
     * vector used.
     * @returns ByteArray: the decrypted data
     */
    fun decrypt(data: SymmetricallyEncryptedData): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, this.key, IvParameterSpec(data.initializationVector))
        return cipher.doFinal(data.encryptedData)
    }

}

/**
 * The SymmetricallyEncryptedData class contains symmetrically encrypted data and the initialization vector used to
 * encrypt it.
 *
 * @property encryptedData: symmetrically encrypted data
 * @property initializationVector: the initialization vector used to encrypt encryptedData
 */
class SymmetricallyEncryptedData(data: ByteArray, iv: ByteArray) {
    val encryptedData = data
    val initializationVector = iv
}

/**
 * Returns a new SymmetricKey object wrapped around an AES-256 SecretKey.
 */
fun newSymmetricKey(): SymmetricKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)
    return SymmetricKey(keyGenerator.generateKey())
}