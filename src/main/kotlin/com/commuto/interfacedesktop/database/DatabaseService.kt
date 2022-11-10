package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*
import com.commuto.interfacedesktop.key.DatabaseKeyDeriver
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.key.keys.SymmetricallyEncryptedData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Database Service Class.
 *
 * This is responsible for storing data, serving it to other services upon request, and accepting services' requests to
 * add and remove data from storage.
 *
 * @property databaseDriverFactory the DatabaseDriverFactory that DatabaseService will use to interact with the
 * database.
 * @property databaseKey The [SymmetricKey] with which this encrypts and decrypts encrypted database fields.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property database The [Database] holding Commuto Interface data.
 * @property databaseServiceContext The single-threaded CoroutineContext in which all database read and write operations
 * are run, in order to prevent data races.
 */
@Singleton
open class DatabaseService(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val databaseKey: SymmetricKey,
) {

    /**
     * Creates a new [DatabaseService] with the given [databaseDriverFactory] and the key created by
     * [DatabaseKeyDeriver] given the password "test_password" and salt string "test_salt". Note this constructor should
     * ONLY be used for testing/development.
     *
     * @param databaseDriverFactory The [DatabaseDriverFactory] that the returned [DatabaseService] will use.
     */
    @Inject constructor(databaseDriverFactory: DatabaseDriverFactory): this(
        databaseDriverFactory = databaseDriverFactory,
        databaseKey = DatabaseKeyDeriver(
            password = "test_password",
            salt = "test_salt".toByteArray(),
        ).key
    )

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
            logger.info("storeOffer: stored offer with B64 ID ${offer.id}")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If an Offer with the specified offer ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storeOffer: offer with B64 ID ${offer.id} already exists in database")
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
            check(dbOffers[0].id == id) {
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
    suspend fun storeSettlementMethods(
        offerID: String,
        chainID: String,
        settlementMethods: List<Pair<String, String?>>
    ) {
        withContext(databaseServiceContext) {
            database.deleteSettlementMethods(offerID, chainID)
            val encoder = Base64.getEncoder()
            for (settlementMethod in settlementMethods) {
                var privateDataString: String? = null
                var initializationVectorString: String? = null
                val privateDataBytes = settlementMethod.second?.encodeToByteArray()
                if (privateDataBytes != null) {
                    val encryptedData = databaseKey.encrypt(privateDataBytes)
                    privateDataString = encoder.encodeToString(encryptedData.encryptedData)
                    initializationVectorString = encoder.encodeToString(encryptedData.initializationVector)
                }
                database.insertSettlementMethod(
                    SettlementMethod(
                        offerID,
                        chainID,
                        settlementMethod.first,
                        privateDataString,
                        initializationVectorString
                    )
                )
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
     * Retrieves the persistently stored settlement methods and their private data (if any) associated with the
     * specified offer ID and chain ID, or returns `null` if no such settlement methods are present.
     *
     * @param offerID The ID of the offer for which settlement methods should be returned, as a Base64-[String] of
     * bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or Swap corresponding to these settlement methods
     * exists, as a [String].
     *
     * @return `null` if no such settlement methods are found, or a [List] of [Pair]s containing a string and an
     * optional string, the first of which is a settlement method associated with [offerID], the second of which is the
     * private data associated with that settlement method, or `null` if no such data is found or if it cannot be
     * decrypted.
     *
     * @throws Exception if database selection is unsuccessful.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getSettlementMethods(offerID: String, chainID: String): List<Pair<String, String?>>? {
        val dbSettlementMethods: List<SettlementMethod> = withContext(databaseServiceContext) {
            database.selectSettlementMethodByOfferIdAndChainID(offerID, chainID)
        }
        return if (dbSettlementMethods.isNotEmpty()) {
            val settlementMethodsList = mutableListOf<Pair<String, String?>>()
            val decoder = Base64.getDecoder()
            dbSettlementMethods.forEach {
                var decryptedPrivateDataString: String? = null
                val privateDataCipherString = it.privateData
                val privateDataInitializationVectorString = it.privateDataInitializationVector
                if (privateDataCipherString != null && privateDataInitializationVectorString != null) {
                    val privateCipherData = try {
                        decoder.decode(privateDataCipherString)
                    } catch (exception: Exception) {
                        null
                    }
                    val privateDataInitializationVector = try {
                        decoder.decode(privateDataInitializationVectorString)
                    } catch (exception: Exception) {
                        null
                    }
                    if (privateCipherData != null && privateDataInitializationVector != null) {
                        val privateDataObject = SymmetricallyEncryptedData(
                            data = privateCipherData,
                            iv = privateDataInitializationVector
                        )
                        val decryptedPrivateData = try {
                            databaseKey.decrypt(privateDataObject)
                        } catch (exception: Exception) {
                            null
                        }
                        if (decryptedPrivateData != null) {
                            decryptedPrivateDataString = decryptedPrivateData.decodeToString()
                        } else {
                            logger.error("getSettlementMethods: unable to get string from decoded private data " +
                                    "using utf8 encoding for offer with B64 ID $offerID")
                        }
                    } else {
                        logger.error("getSettlementMethods: found private data for offer with B64 ID $offerID " +
                                "but could not decode bytes from strings")
                    }
                } else {
                    logger.error("getSettlementMethods: did not find private settlement method data for " +
                            "settlement method for offer with B64 ID $offerID")
                }
                settlementMethodsList.add(Pair(it.settlementMethod, decryptedPrivateDataString))
            }
            logger.info("getSettlementMethods: returning ${dbSettlementMethods.size} for offer with B64 ID " +
                    offerID)
            return settlementMethodsList
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
        val encryptedMakerData = encryptPrivateSwapSettlementMethodData(
            privateSettlementMethodData = swap.makerPrivateData
        )
        val encryptedTakerData = encryptPrivateSwapSettlementMethodData(
            privateSettlementMethodData = swap.takerPrivateData
        )
        val swapToInsert = Swap(
            id = swap.id,
            isCreated = swap.isCreated,
            requiresFill = swap.requiresFill,
            maker = swap.maker,
            makerInterfaceID = swap.makerInterfaceID,
            taker = swap.taker,
            takerInterfaceID = swap.takerInterfaceID,
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound,
            amountUpperBound = swap.amountUpperBound,
            securityDepositAmount = swap.securityDepositAmount,
            takenSwapAmount = swap.takenSwapAmount,
            serviceFeeAmount = swap.serviceFeeAmount,
            serviceFeeRate = swap.serviceFeeRate,
            onChainDirection = swap.onChainDirection,
            settlementMethod = swap.settlementMethod,
            makerPrivateData = encryptedMakerData.first,
            makerPrivateDataInitializationVector = encryptedMakerData.second,
            takerPrivateData = encryptedTakerData.first,
            takerPrivateDataInitializationVector = encryptedTakerData.second,
            protocolVersion = swap.protocolVersion,
            isPaymentSent = swap.isPaymentSent,
            isPaymentReceived = swap.isPaymentReceived,
            hasBuyerClosed = swap.hasBuyerClosed,
            hasSellerClosed = swap.hasSellerClosed,
            disputeRaiser = swap.disputeRaiser,
            chainID = swap.chainID,
            state = swap.state,
            role = swap.role
        )
        try {
            withContext(databaseServiceContext) {
                database.insertSwap(swapToInsert)
            }
            logger.info("storeSwap: stored swap with B64 ID ${swap.id}")
        } catch (exception: SQLiteException) {
            /*
            The result code for a UNIQUE constraint failure; see here: https://www.sqlite.org/rescode.html
            If a Swap with the specified ID already exists, we do nothing.
             */
            if (exception.resultCode.code != 2067) {
                throw exception
            }
            logger.info("storeSwap: swap with B64 ID ${swap.id} already exists in database")
        }
    }

    /**
     * Encrypts [privateSettlementMethodData] data with [databaseKey] along with the initialization vector used for
     * encryption, and returns both as Base64 encoded strings of bytes within a pair, or returns a pair of `null` values
     * if [privateSettlementMethodData] is `null`.
     *
     * @param privateSettlementMethodData The string of private data to be symmetrically encrypted with [databaseKey].
     * @return A [Pair] containing two optional strings. If [privateSettlementMethodData] is not `null`, the first will
     * be [privateSettlementMethodData] encrypted with [databaseKey] and a new initialization vector as a Base64 encoded
     * string of bytes, and the second will be the initialization vector used to encrypt [privateSettlementMethodData],
     * also as a Base64 encoded string of bytes. If [privateSettlementMethodData] is `null`, both strings will be
     * `null`.
     */
    private fun encryptPrivateSwapSettlementMethodData(privateSettlementMethodData: String?): Pair<String?, String?> {
        var privateCipherDataString: String? = null
        var initializationVectorString: String? = null
        val privateByteArray = privateSettlementMethodData?.toByteArray()
        if (privateByteArray != null) {
            val encryptedData = databaseKey.encrypt(privateByteArray)
            val encoder = Base64.getEncoder()
            privateCipherDataString = encoder.encodeToString(encryptedData.encryptedData)
            initializationVectorString = encoder.encodeToString(encryptedData.initializationVector)
        }
        return Pair(privateCipherDataString, initializationVectorString)
    }

    /**
     * Updates the [Swap.requiresFill] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param requiresFill The new value of the swap's [Swap.requiresFill] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapRequiresFill(swapID: String, chainID: String, requiresFill: Boolean) {
        val requiresFillLong = if (requiresFill) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateSwapRequiresFill(swapID, chainID, requiresFillLong)
        }
        logger.info("updateSwapRequiresFill: set value to $requiresFill for swap with B64 ID $swapID, if " +
                "present")
    }

    /**
     * Updates the [Swap.isPaymentSent] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param isPaymentSent The new value of the swap's [Swap.isPaymentSent] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapIsPaymentSent(swapID: String, chainID: String, isPaymentSent: Boolean) {
        val isPaymentSentLong = if (isPaymentSent) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateSwapIsPaymentSent(swapID, chainID, isPaymentSentLong)
        }
        logger.info("updateSwapIsPaymentSent: set value to $isPaymentSent for swap with B64 ID $swapID, if " +
                "present")
    }

    /**
     * Updates the [Swap.isPaymentReceived] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param isPaymentReceived The new value of the swap's [Swap.isPaymentReceived] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapIsPaymentReceived(swapID: String, chainID: String, isPaymentReceived: Boolean) {
        val isPaymentReceivedLong = if (isPaymentReceived) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateSwapIsPaymentReceived(swapID, chainID, isPaymentReceivedLong)
        }
        logger.info("updateSwapIsPaymentReceived: set value to $isPaymentReceived for swap with B64 ID $swapID, if " +
                "present")
    }

    /**
     * Updates the [Swap.hasBuyerClosed] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param hasBuyerClosed The new value of the swap's [Swap.hasBuyerClosed] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapHasBuyerClosed(swapID: String, chainID: String, hasBuyerClosed: Boolean) {
        val hasBuyerClosedLong = if (hasBuyerClosed) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateSwapHasBuyerClosed(swapID, chainID, hasBuyerClosedLong)
        }
        logger.info("updateSwapHasBuyerClosed: set value to $hasBuyerClosed for swap with B64 ID $swapID, if " +
                "present")
    }

    /**
     * Updates the [Swap.hasSellerClosed] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param hasSellerClosed The new value of the swap's [Swap.hasSellerClosed] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapHasSellerClosed(swapID: String, chainID: String, hasSellerClosed: Boolean) {
        val hasSellerClosedLong = if (hasSellerClosed) 1L else 0L
        withContext(databaseServiceContext) {
            database.updateSwapHasSellerClosed(swapID, chainID, hasSellerClosedLong)
        }
        logger.info("updateSwapHasSellerClosed: set value to $hasSellerClosed for swap with B64 ID $swapID, if " +
                "present")
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

    // TODO: Update this
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
            check(dbSwaps[0].id == id) {
                "Returned swap ID ${dbSwaps[0].id} did not match specified swap ID $id"
            }
            val decryptedMakerPrivateData = decryptPrivateSwapSettlementMethodData(
                privateSettlementMethodData = dbSwaps[0].makerPrivateData,
                privateSettlementMethodDataInitializationVector = dbSwaps[0].makerPrivateDataInitializationVector
            )
            val decryptedTakerPrivateData = decryptPrivateSwapSettlementMethodData(
                privateSettlementMethodData = dbSwaps[0].takerPrivateData,
                privateSettlementMethodDataInitializationVector = dbSwaps[0].takerPrivateDataInitializationVector
            )
            logger.info("getSwap: returning swap with B64 ID $id")
            Swap(
                id = dbSwaps[0].id,
                isCreated = dbSwaps[0].isCreated,
                requiresFill = dbSwaps[0].requiresFill,
                maker = dbSwaps[0].maker,
                makerInterfaceID = dbSwaps[0].makerInterfaceID,
                taker = dbSwaps[0].taker,
                takerInterfaceID = dbSwaps[0].takerInterfaceID,
                stablecoin = dbSwaps[0].stablecoin,
                amountLowerBound = dbSwaps[0].amountLowerBound,
                amountUpperBound = dbSwaps[0].amountUpperBound,
                securityDepositAmount = dbSwaps[0].securityDepositAmount,
                takenSwapAmount = dbSwaps[0].takenSwapAmount,
                serviceFeeAmount = dbSwaps[0].serviceFeeAmount,
                serviceFeeRate = dbSwaps[0].serviceFeeRate,
                onChainDirection = dbSwaps[0].onChainDirection,
                settlementMethod = dbSwaps[0].settlementMethod,
                makerPrivateData = decryptedMakerPrivateData,
                makerPrivateDataInitializationVector = null,
                takerPrivateData = decryptedTakerPrivateData,
                takerPrivateDataInitializationVector = null,
                protocolVersion = dbSwaps[0].protocolVersion,
                isPaymentSent = dbSwaps[0].isPaymentSent,
                isPaymentReceived = dbSwaps[0].isPaymentReceived,
                hasBuyerClosed = dbSwaps[0].hasBuyerClosed,
                hasSellerClosed = dbSwaps[0].hasSellerClosed,
                disputeRaiser = dbSwaps[0].disputeRaiser,
                chainID = dbSwaps[0].chainID,
                state = dbSwaps[0].state,
                role = dbSwaps[0].role,
            )
        } else {
            logger.info("getSwap: no swap found with B64 ID $id")
            null
        }
    }

    private fun decryptPrivateSwapSettlementMethodData(
        privateSettlementMethodData: String?,
        privateSettlementMethodDataInitializationVector: String?
    ): String? {
        var decryptedPrivateDataString: String? = null
        if (privateSettlementMethodData != null && privateSettlementMethodDataInitializationVector != null) {
            val decoder = Base64.getDecoder()
            val privateCipherData = try {
                decoder.decode(privateSettlementMethodData)
            } catch (exception: Exception) {
                null
            }
            val privateDataInitializationVector = try {
                decoder.decode(privateSettlementMethodDataInitializationVector)
            } catch (exception: Exception) {
                null
            }
            if (privateCipherData != null && privateDataInitializationVector != null) {
                val privateDataObject = SymmetricallyEncryptedData(
                    data = privateCipherData,
                    iv = privateDataInitializationVector
                )
                try {
                    databaseKey.decrypt(privateDataObject)
                } catch(exception: Exception) {
                    return null
                }.let {
                    decryptedPrivateDataString = it.decodeToString()
                }
            }
        }
        return decryptedPrivateDataString
    }

}