package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Database Service Class.
 *
 * This is responsible for storing data, serving it to other services upon request, and accepting services' requests to
 * add and remove data from storage.
 *
 * @property databaseDriverFactory the DatabaseDriverFactory that DBService will use to interact with the database.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property database The [Database] holding Commuto Interface data.
 * @property databaseServiceContext The single-threaded CoroutineContext in which all database read and write operations
 * are run, in order to prevent data races.
 */
@Singleton
open class DatabaseService @Inject constructor(private val databaseDriverFactory: DatabaseDriverFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
     * Persistently stores an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). If an Offer
     * with the specified offer ID already exists in the database, this does nothing.
     *
     * @param offer The [Offer] to be stored in the database.
     *
     * @throws Exception if database insertion is unsuccessful for a reason OTHER than UNIQUE constraint failure.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storeOffer(offer: Offer) {
        try {
            withContext(databaseServiceContext) {
                database.insertOffer(offer)
            }
            logger.info("storeOffer: stored offer with B64 ID ${offer.offerId}")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If an Offer with the specified offer ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storeOffer: offer with B64 ID ${offer.offerId} already exists in database")
        }
    }

    /**
     * Updates the [Offer.havePublicKey] property of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param havePublicKey The new value of the offer's [Offer.havePublicKey] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    open suspend fun updateOfferHavePublicKey(offerID: String, chainID: String, havePublicKey: Boolean) {
        val havePublicKeyLong = if (havePublicKey) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateOfferHavePublicKey(offerID, chainID, havePublicKeyLong)
        }
        logger.info("updateOfferHavePublicKey: set value to $havePublicKey for offer with B64 ID $offerID, if " +
                "present")
    }

    /**
     * Updates the [Offer.state] property of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param state The new value of the offer's [Offer.state] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    open suspend fun updateOfferState(offerID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateOfferState(offerID, chainID, state)
        }
        logger.info("updateOfferState: set value to $state for offer with B64 ID $offerID, if present")
    }

    /**
     * Removes every [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with an offer ID equal
     * to [offerID] and a chain ID equal to [chainID] from persistent storage.
     *
     * @param offerID The offer ID of the offers to be removed, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offers to be removed, as a [String].
     *
     * @throws Exception If deletion is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun deleteOffers(offerID: String, chainID: String) {
        withContext(databaseServiceContext) {
            database.deleteOffer(offerID, chainID)
        }
        logger.info("deleteOffers: deleted offers with B64 ID $offerID and chain ID $chainID, if present")
    }

    /**
     * Retrieves the persistently stored [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)
     * with the given offer ID, or returns null if no such offer is present.
     *
     * @param id The offer ID of the Offer to return, as a Base64-[String] of bytes.
     *
     * @throws IllegalStateException if multiple offers are found for a single offer ID, or if the offer ID of the offer
     * returned from the database query does not match [id].
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getOffer(id: String): Offer? {
        val dbOffers: List<Offer> = withContext(databaseServiceContext) {
            database.selectOfferByOfferId(id)
        }
        return if (dbOffers.size > 1) {
            throw IllegalStateException("Multiple offers found with given offer id $id")
        } else if (dbOffers.size == 1) {
            check(dbOffers[0].offerId == id) {
                "Returned offer id $id did not match specified offer id $id"
            }
            logger.info("getOffer: returning offer with B64 ID $id")
            dbOffers[0]
        } else {
            logger.info("getOffer: no offer found with B64 ID $id")
            null
        }
    }

    /**
     * Deletes all persistently stored settlement methods associated with the specified ID, and then persistently stores
     * each settlement method in the supplied [List], associating each one with the supplied ID.
     *
     * @param offerID The ID of the offer or swap to be associated with the settlement methods.
     * @param settlementMethods The settlement methods to be persistently stored.
     *
     * @throws Exception if database insertion is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storeSettlementMethods(offerID: String, chainID: String, settlementMethods: List<String>) {
        withContext(databaseServiceContext) {
            database.deleteSettlementMethods(offerID, chainID)
            for (settlementMethod in settlementMethods) {
                database.insertSettlementMethod(SettlementMethod(offerID, chainID, settlementMethod))
            }
        }
        logger.info("storeSettlementMethods: stored ${settlementMethods.size} for offer with B64 ID $offerID")
    }

    /**
     * Removes every persistently stored settlement method associated with an offer ID equal to [offerID] and a
     * blockchain ID equalt to [chainID].
     *
     * @param offerID The ID of the offer for which associated settlement methods should be removed, as a
     * Base64-[String] of bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or `Swap` corresponding to these settlement methods
     * exists, as a [String].
     *
     * @throws Exception If deletion is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun deleteSettlementMethods(offerID: String, chainID: String) {
        withContext(databaseServiceContext) {
            database.deleteSettlementMethods(offerID, chainID)
        }
        logger.info("deleteSettlementMethods: deleted for offer with B64 ID $offerID")
    }

    /**
     * Retrieves the persistently stored settlement methods associated with the specified offer ID and chain ID, or
     * returns null if no such settlement methods are present.
     *
     * @param offerID The ID of the offer for which settlement methods should be returned, as a Base64-[String] of bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or Swap corresponding to these settlement methods
     * exists, as a [String].
     *
     * @return A [List] of [String]s which are settlement methods associated with [offerID] and [chainID], or null if no
     * such settlement methods are found.
     *
     * @throws Exception if database selection is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getSettlementMethods(offerID: String, chainID: String): List<String>? {
        val dbSettlementMethods: List<SettlementMethod> = withContext(databaseServiceContext) {
            database.selectSettlementMethodByOfferIdAndChainID(offerID, chainID)
        }
        return if (dbSettlementMethods.isNotEmpty()) {
            logger.info("getSettlementMethods: returning ${dbSettlementMethods.size} for offer with B64 ID $offerID")
            dbSettlementMethods.map {
                it.settlementMethod
            }
        } else {
            logger.info("getSettlementMethods: none found for offer with B64 ID $offerID")
            null
        }
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
            logger.info("storeKeyPair: stored with interface ID $interfaceId")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a key pair with the specified interface ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storeKeyPair: key pair with interface ID $interfaceId already exists in database")
        }
    }

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
            logger.info("getKeyPair: returning key pair with interface ID $interfaceId")
            KeyPair(interfaceId, dbKeyPairs[0].publicKey, dbKeyPairs[0].privateKey)
        } else {
            logger.info("getKeyPair: no key pair found with interface ID $interfaceId")
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
            logger.info("storePublicKey: stored with interface ID $interfaceId")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a public key with the specified interface ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storePublicKey: public key with interface ID $interfaceId already exists in database")
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
            logger.info("getPublicKey: returning public key with interface ID $interfaceId")
            PublicKey(interfaceId, dbPublicKeys[0].publicKey)
        } else {
            logger.info("getPublicKey: no public key found with interface ID $interfaceId")
            null
        }
    }

    /**
     * Persistently stores a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap). If a Swap with
     * the specified ID already exists in the database, this does nothing.
     *
     * @param swap The [Swap] to be stored in the database.
     *
     * @throws Exception if database insertion is unsuccessful for a reason OTHER than UNIQUE constraint failure.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storeSwap(swap: Swap) {
        try {
            withContext(databaseServiceContext) {
                database.insertSwap(swap)
            }
            logger.info("storeSwap: stored swap with B64 ID ${swap.swapID}")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a Swap with the specified ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storeSwap: swap with B64 ID ${swap.swapID} already exists in database")
        }
    }
    /**
     * Updates the [Swap.state] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param state The new value of the swap's [Swap.state] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapState(swapID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateSwapState(swapID, chainID, state)
        }
        logger.info("updateSwapState: set value to $state for swap with B64 ID $swapID, if present")
    }

    /**
     * Removes every [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with a swap ID equal to
     * [swapID] and a chain ID equal to [chainID] from persistent storage.
     *
     * @param swapID The ID of the swaps to be removed, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swaps to be removed, as a [String].
     *
     * @throws Exception If deletion is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun deleteSwaps(swapID: String, chainID: String) {
        withContext(databaseServiceContext) {
            database.deleteSwap(swapID, chainID)
        }
        logger.info("deleteSwaps: deleted swap with B64 ID $swapID and chain ID $chainID, if present")
    }

    /**
     * Retrieves the persistently stored [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with
     * the given ID, or returns null if no such swap is present.
     *
     * @param id The ID of the Swap to return, as a Base64-[String] of bytes.
     *
     * @throws IllegalStateException if multiple swaps are found for a single ID, or if the ID of the swap returned from
     * the database query does not match [id].
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getSwap(id: String): Swap? {
        val dbSwaps: List<Swap> = withContext(databaseServiceContext) {
            database.selectSwapBySwapID(id)
        }
        return if (dbSwaps.size > 1) {
            throw IllegalStateException("Multiple swaps found with given id $id")
        } else if (dbSwaps.size == 1) {
            check(dbSwaps[0].swapID == id) {
                "Returned swap id $id did not match specified swap id $id"
            }
            logger.info("getSwap: returning swap with B64 ID $id")
            dbSwaps[0]
        } else {
            logger.info("getSwap: no swap found with B64 ID $id")
            null
        }
    }

}