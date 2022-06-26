package com.commuto.interfacedesktop.keymanager

import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.keymanager.types.KeyPair
import com.commuto.interfacedesktop.keymanager.types.PublicKey
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.Base64

/**
 * The Key Manager Service Class.
 *
 * This is responsible for generating, persistently storing and retrieving key pairs, and for storing and retrieving
 * public keys.
 *
 * @property databaseService The Database Service used to store and retrieve data
 */
class KMService(var databaseService: DatabaseService) {

    /**
     * Generates an 2048-bit RSA key pair and computes the key pair's interface ID, which is the SHA-256 hash of the
     * PKCS#1 byte array encoded representation of the public key.
     *
     * @param storeResult Indicates whether the generated key pair should be stored persistently using [databaseService].
     * This will usually be left as true, but testing code may set it as false.
     *
     * @return A [KeyPair], the private key of which is a 2048-bit RSA private key, and the interface ID of which is the
     * SHA-256 hash of the PKCS#1 encoded byte representation of the [KeyPair]'s public key.
     */
    fun generateKeyPair(storeResult: Boolean = true): KeyPair {
        val keyPair = KeyPair()
        val encoder = Base64.getEncoder()
        if (storeResult) {
            runBlocking {
                databaseService.storeKeyPair(encoder.encodeToString(keyPair.interfaceId),
                    encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()),
                    encoder.encodeToString(keyPair.privKeyToPkcs1Bytes()))
            }
        }
        return keyPair
    }

    /**
     * Retrieves the persistently stored [KeyPair] associated with the specified interface ID, or returns null if such a
     * [KeyPair] is not found.
     *
     * @param interfaceId The interface ID of the desired [KeyPair], which is the SHA-256 hash of the PKCS#1 byte
     * encoded representation of the [KeyPair]'s public key.
     *
     * @return A [KeyPair], the private key of which is a 2048-bit RSA key pair such that the SHA-256 hash of the PKCS#1
     * encoded byte representation of its public key is equal to [interfaceId], or null if no such [KeyPair] is found.
     */
    fun getKeyPair(interfaceId: ByteArray): KeyPair? {
        val encoder = Base64.getEncoder()
        val dbKeyPair: com.commuto.interfacedesktop.db.KeyPair? = runBlocking {
            databaseService
                .getKeyPair(encoder.encodeToString(interfaceId))
        }
        val decoder = Base64.getDecoder()
        if (dbKeyPair != null) {
            return KeyPair(decoder.decode(dbKeyPair.publicKey), decoder.decode(dbKeyPair.privateKey))
        } else {
            return null
        }
    }

    /**
     * Persistently stores a [PublicKey] in such a way that it can be retrieved again given its interface ID. This will
     * generate a PKCS#1 byte representation of [pubKey], encode it as a Base64 [String] and store it using [databaseService].
     *
     * @param pubKey The [PublicKey] to be persistently stored.
     */
    fun storePublicKey(pubKey: PublicKey) {
        val interfaceId: ByteArray = MessageDigest.getInstance("SHA-256")
            .digest(pubKey.toPkcs1Bytes())
        val encoder = Base64.getEncoder()
        runBlocking {
            databaseService.storePublicKey(encoder.encodeToString(interfaceId),
                encoder.encodeToString(pubKey.toPkcs1Bytes()))
        }
    }

    /**
     * Retrieves the persistently stored [PublicKey] with an interface ID equal to [interfaceId], or returns nill if no
     * such [PublicKey] is found.
     *
     * @param interfaceId The interface ID of the desired [PublicKey]. This is the SHA-256 hash of the desired
     * [PublicKey]'s PKCS#1 byte encoded representation.
     *
     * @return A [PublicKey], the SHA-256 hash of the PKCS#1 encoded byte representation of which is equal to
     * [interfaceId], or null if no such [PublicKey] is found.
     */
    fun getPublicKey(interfaceId: ByteArray): PublicKey? {
        val encoder = Base64.getEncoder()
        val dbPubKey: com.commuto.interfacedesktop.db.PublicKey? = runBlocking {
            databaseService.getPublicKey(encoder.encodeToString(interfaceId))
        }
        val decoder = Base64.getDecoder()
        if (dbPubKey != null) {
            return PublicKey(decoder.decode(dbPubKey.publicKey))
        } else {
            return null
        }
    }

}