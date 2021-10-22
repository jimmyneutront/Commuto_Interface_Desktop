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
 * @param databaseDriverFactory the DatabaseDriverFactory that DBService will use to interact with the database.
 */
class DBService(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)

    /**
     * Creates all necessary tables for proper operation.
     */
    fun createTables() {
        database.createTables()
    }

    /**
     * Persistently stores a key pair associated with an interface id.
     */
    //TODO: Update this to use actual Commuto Interface custom types, instead of strings
    fun storeKeyPair(interfaceId: String, publicKey: String, privateKey: String) {
        val keyPair = KeyPair(interfaceId, publicKey, privateKey)
        database.insertKeyPair(keyPair)
    }

    /**
     * Retrieves all persistently stored key pairs associated with the given interface id.
     */
    //TODO: Update this to use actual Commuto Interface custom types, instead of strings
    fun getKeyPair(interfaceId: String): List<KeyPair> {
        return database.selectKeyPairByInterfaceId(interfaceId)
    }

    /**
     * Persistently stores a public key associated with an interface id.
     */
    //TODO: Update this to use actual Commuto Interface custom types, instead of strings
    fun storePublicKey(interfaceId: String, publicKey: String) {
        val publicKey = PublicKey(interfaceId, publicKey)
        database.insertPublicKey(publicKey)
    }

    /**
     * Retrieves all persistently stored public keys associated with the given interface id
     */
    //TODO: Update this to use actual Commuto Interface custom types, instead of strings
    fun getPublicKey(interfaceId: String): List<PublicKey> {
        return database.selectPublicKeyByInterfaceId(interfaceId)
    }

}