package com.commuto.interfacedesktop.dbService

import com.commuto.interfacedesktop.db.Database
import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.db.KeyPair
import com.commuto.interfacedesktop.db.PublicKey

/**
 * The Database Service Class
 *
 * This is responsible for storing data, serving it to Local Services upon request, and accepting Local Services'
 * requests to add and remove data from storage.
 *
 * @property databaseDriverFactory the DatabaseDriverFactory that DBService will use to interact with the database.
 */
class DBService(val databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    //TODO: Localize error strings
    /**
     * Creates all necessary tables for proper operation.
     */
    fun createTables() {
        database.createTables()
    }

    /**
     * Clears entire database
     */
    fun clearDatabase() {
        database.clearDatabase()
    }

    /**
     * Persistently stores a key pair associated with an interface id.
     *
     * @param interfaceId the interface id of the keypair as a byte array encoded to a hexadecimal String
     * @param publicKey the public key of the keypair as a byte array encoded to a hexadecimal String
     * @param privateKey the private key of the keypair as a byte array encoded to a hexadecimal String
     *
     * Used exclusively by KMService
     */
    fun storeKeyPair(interfaceId: String, publicKey: String, privateKey: String) {
        val keyPair = KeyPair(interfaceId, publicKey, privateKey)
        if (getKeyPair(interfaceId) != null) {
            throw IllegalStateException("Database query for key pair with interface id " + interfaceId +
                    " returned result")
        } else {
            database.insertKeyPair(keyPair)
        }
    }

    /**
     * Retrieves the persistently stored key pair associated with the given interface id, or null if not present.
     *
     * @param interfaceId the interface id of the keypair as a byte array encoded to a hexadecimal String
     *
     * @return KeyPair object (defined in com.commuto.interfacedesktop.db) if key pair is found
     * @return null if key pair is not found
     * @throws IllegalStateException if multiple key pairs are found for a given interface id, or if the interface id
     * returned from the database query does not match the interface id passed to getKeyPair
     *
     * Used exclusively by KMService
     */
    fun getKeyPair(interfaceId: String): KeyPair? {
        val dbKeyPairs: List<KeyPair> = database.selectKeyPairByInterfaceId(interfaceId)
        if (dbKeyPairs.size > 1) {
            throw IllegalStateException("Multiple key pairs found with given interface id " + interfaceId)
        } else if (dbKeyPairs.size == 1) {
            check(dbKeyPairs[0].interfaceId.equals(interfaceId)) {
                "Returned interface id " + dbKeyPairs[0].interfaceId + "did not match specified interface id " +
                        interfaceId
            }
            return KeyPair(interfaceId, dbKeyPairs[0].publicKey, dbKeyPairs[0].privateKey)
        } else {
            return null
        }
    }

    /**
     * Persistently stores a public key associated with an interface id.
     *
     * @param interfaceId the interface id of the keypair as a byte array encoded to a hexadecimal String
     * @param publicKey the public key to be stored as a byte array encoded to a hexadecimal String
     *
     * @throws IllegalStateException if a public key with the given interface id is already found in the database
     */
    fun storePublicKey(interfaceId: String, publicKey: String) {
        val publicKey = PublicKey(interfaceId, publicKey)
        if (getPublicKey(interfaceId) != null) {
            throw IllegalStateException("Database query for public key with interface id " + interfaceId +
                    " returned result")
        } else {
            database.insertPublicKey(publicKey)
        }
    }

    /**
     * Retrieves the persistently stored public key associated with the given interface id, or null if not present
     *
     * @param interfaceId the interface id of the public key as a byte array encoded to a hexadecimal String
     *
     * @return PublicKey object (definded in com.commuto.interfacedesktop.db) if public key is found
     * @return null if public key is not found
     * @throws IllegalStateException if multiple public keys are found for a given interface id, or if the interface id
     * returned from the database query does not match the interface id passed to getPublicKey
     *
     * Used exclusively by KMService
     */
    fun getPublicKey(interfaceId: String): PublicKey? {
        val dbPublicKeys: List<PublicKey> = database.selectPublicKeyByInterfaceId(interfaceId)
        if (dbPublicKeys.size > 1) {
            throw IllegalStateException("Multiple public keys found with given interface id " + interfaceId)
        } else if (dbPublicKeys.size == 1) {
            check(dbPublicKeys[0].interfaceId.equals(interfaceId)) {
                "Returned interface id " + dbPublicKeys[0].interfaceId + "did not match specified interface id " +
                        interfaceId
            }
            return PublicKey(interfaceId, dbPublicKeys[0].publicKey)
        } else {
            return null
        }
    }

}