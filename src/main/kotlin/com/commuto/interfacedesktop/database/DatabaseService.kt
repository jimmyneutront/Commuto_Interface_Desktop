package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.KeyPair
import com.commuto.interfacedesktop.db.PublicKey
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.sqlite.SQLiteException

// TODO: Document Database and DatabaseDriverFactory

/**
 * The Database Service Class.
 *
 * This is responsible for storing data, serving it to other services upon request, and accepting services' requests to
 * add and remove data from storage.
 *
 * @property databaseDriverFactory the DatabaseDriverFactory that DBService will use to interact with the database.
 * @property database The [Database] holding Commuto Interface data.
 * @property databaseServiceContext The single-threaded CoroutineContext in which all database read and write operations
 * are run, in order to prevent data races.
 */
class DatabaseService(private val databaseDriverFactory: DatabaseDriverFactory) {

    private val database = Database(databaseDriverFactory)
    // We want to run all database operations on a single thread to prevent data races.
    @DelicateCoroutinesApi
    private val databaseServiceContext = newSingleThreadContext("DatabaseServiceContext")

    /**
     * Creates all necessary database tables.
     */
    fun createTables() {
        database.createTables()
    }

    /**
     * Clears the entire [database].
     */
    fun clearDatabase() {
        database.clearDatabase()
    }

    /**
     * Persistently stores a key pair associated with an interface id. If a key pair with the specified interface ID
     * already exists in the database, this does nothing.
     *
     * @param interfaceId The interface ID of the key pair as a byte array encoded to a hexadecimal [String].
     * @param publicKey The public key of the key pair as a byte array encoded to a hexadecimal [String].
     * @param privateKey The private key of the key pair as a byte array encoded to a hexadecimal [String].
     *
     * @throws Exception if database insertion is unsuccessful for a reason OTHER than UNIQUE constraint failure.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storeKeyPair(interfaceId: String, publicKey: String, privateKey: String) {
        val keyPair = KeyPair(interfaceId, publicKey, privateKey)
        try {
            withContext(databaseServiceContext) {
                database.insertKeyPair(keyPair)
            }
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a key pair with the specified interface ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
        }
    }
    /*
    fun storeKeyPair(interfaceId: String, publicKey: String, privateKey: String) {
        val keyPair = KeyPair(interfaceId, publicKey, privateKey)
        database.insertKeyPair(keyPair)
    }*/

    /**
     * Retrieves the persistently stored key pair associated with the given interface ID, or returns null if no such key
     * pair is present.
     *
     * @param interfaceId The interface ID of the desired key pair as a byte array encoded to a hexadecimal [String].
     *
     * @return A [KeyPair] if key pair with the specified interface ID is found, or null if such a key pair is not
     * found.
     *
     * @throws IllegalStateException if multiple key pairs are found for a single interface ID, or if the interface ID
     * of the key pair returned from the database query does not match [interfaceId].
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getKeyPair(interfaceId: String): KeyPair? {
        val dbKeyPairs: List<KeyPair> = withContext(databaseServiceContext) {
            database.selectKeyPairByInterfaceId(interfaceId)
        }
        return if (dbKeyPairs.size > 1) {
            throw IllegalStateException("Multiple key pairs found with given interface id $interfaceId")
        } else if (dbKeyPairs.size == 1) {
            check(dbKeyPairs[0].interfaceId == interfaceId) {
                "Returned interface id " + dbKeyPairs[0].interfaceId + "did not match specified interface id " +
                        interfaceId
            }
            KeyPair(interfaceId, dbKeyPairs[0].publicKey, dbKeyPairs[0].privateKey)
        } else {
            null
        }
    }

    /**
     * Persistently stores a public key associated with an interface ID.
     *
     * @param interfaceId The interface ID of the key pair as a byte array encoded to a hexadecimal [String].
     * @param publicKey The public key to be stored, as a byte array encoded to a hexadecimal [String].
     *
     * @throws IllegalStateException if a public key with the given interface ID is already found in the database.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storePublicKey(interfaceId: String, publicKey: String) {
        val databasePublicKey = PublicKey(interfaceId, publicKey)
        try {
            withContext(databaseServiceContext) {
                database.insertPublicKey(databasePublicKey)
            }
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a public key with the specified interface ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
        }
    }

    /**
     * Retrieves the persistently stored public key associated with the given interface ID, or returns nul if no such
     * key is found.
     *
     * @param interfaceId The interface ID of the public key as a byte array encoded to a hexadecimal [String].
     *
     * @return A [PublicKey] if a public key with the specified interface ID is found, or null if no such public key is
     * found.
     *
     * @throws IllegalStateException if multiple public keys are found for a given interface ID, or if the interface ID
     * of the public key returned from the database query does not match [interfaceId].
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getPublicKey(interfaceId: String): PublicKey? {
        val dbPublicKeys: List<PublicKey> = withContext(databaseServiceContext) {
            database.selectPublicKeyByInterfaceId(interfaceId)
        }
        return if (dbPublicKeys.size > 1) {
            throw IllegalStateException("Multiple public keys found with given interface id $interfaceId")
        } else if (dbPublicKeys.size == 1) {
            check(dbPublicKeys[0].interfaceId == interfaceId) {
                "Returned interface id " + dbPublicKeys[0].interfaceId + "did not match specified interface id " +
                        interfaceId
            }
            PublicKey(interfaceId, dbPublicKeys[0].publicKey)
        } else {
            null
        }
    }

}