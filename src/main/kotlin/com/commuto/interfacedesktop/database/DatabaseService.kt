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
     * Updates the [Offer.cancelingOfferState] property of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param state The new value of the offer's [Offer.cancelingOfferState] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateCancelingOfferState(offerID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateCancelingOfferState(offerID, chainID, state)
        }
        logger.info("updateCancelingOfferState: set value to $state for offer with B64 ID $offerID, if present")
    }

    /**
     * Updates the [Offer.offerCancellationTransactionHash], [Offer.offerCancellationTransactionCreationTime] and
     * [Offer.offerCancellationTransactionCreationBlockNumber] properties of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param transactionHash The new value of the offer's [Offer.offerCancellationTransactionHash] property, as a
     * transaction hash as a hexadecimal string with "0x" prefix.
     * @param creationTime The new value of the offer's [Offer.offerCancellationTransactionCreationTime] property.
     * @param blockNumber The new value of the offer's [Offer.offerCancellationTransactionCreationBlockNumber] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateOfferCancellationData(
        offerID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        withContext(databaseServiceContext) {
            database.updateOfferCancellationData(
                offerID = offerID,
                chainID = chainID,
                transactionHash = transactionHash,
                creationTime = creationTime,
                blockNumber = blockNumber,
            )
        }
        logger.info("updateOfferCancellationData: set values to $transactionHash, $creationTime and $blockNumber for " +
                "offer with B64 ID $offerID, if present")
    }

    /**
     * Updates the [Offer.editingOfferState] property of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param state The new value of the offer's [Offer.editingOfferState] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateEditingOfferState(offerID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateEditingOfferState(offerID, chainID, state)
        }
        logger.info("updateEditingOfferState: set value to $state for offer with B64 ID $offerID, if present")
    }

    /**
     * Updates the [Offer.offerEditingTransactionHash] property of a persistently stored
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) with the specified [offerID] and
     * [chainID].
     *
     * @param offerID The ID of the offer to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the offer to be updated, as a [String].
     * @param transactionHash The new value of the offer's [Offer.offerEditingTransactionHash] property, as a
     * transaction hash as a hexadecimal string with "0x" prefix.
     * @param creationTime The new value of the offer's [Offer.offerEditingTransactionCreationTime] property.
     * @param blockNumber The new value of the offer's [Offer.offerEditingTransactionCreationBlockNumber] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateOfferEditingData(
        offerID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        withContext(databaseServiceContext) {
            database.updateOfferEditingData(
                offerID = offerID,
                chainID = chainID,
                transactionHash = transactionHash,
                creationTime = creationTime,
                blockNumber = blockNumber,
            )
        }
        logger.info("updateOfferEditingTransactionHash: set values to $transactionHash, $creationTime and " +
                "$blockNumber for offer with B64 ID $offerID, if present")
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
     * Deletes all persistently stored settlement methods, their private data and corresponding initialization vectors
     * (if any) associated with the specified offer ID and chain ID in a database table via [deletionLambda], and then
     * persistently stores each settlement method and private data string tuples in the supplied [List] into the same
     * database table via [insertionLambda], associating each one with the supplied offer ID and chain ID. Private
     * settlement method data is encrypted with [databaseKey] and a new initialization vector.
     *
     * @param offerID The ID of the offer or swap to be associated with the settlement methods.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     * @param settlementMethods The settlement methods to be persistently stored, as a [List] of [Pair], each of which
     * contains a string and an optional string, in that order. The first element in a [Pair] is the public settlement
     * method data, including price, currency and type. The second element element in a [Pair] is private data (such as
     * an address or bank account number) for the settlement method, if any.
     * @param deletionLambda A lambda that can be executed to delete all settlement methods with a given offer ID and
     * chainID from the same database table into which this will insert new settlement methods. This lambda will be
     * executed on the [databaseServiceContext] coroutine dispatcher.
     * @param insertionLambda A lambda that can be executed to insert a new row, containing an offer ID, a chain ID,
     * public settlement method data, encrypted private settlement method data, and an initialization vector, into the
     * same table from which [deletionLambda] deletes settlement methods. This lambda will be executed on the
     * [databaseServiceContext] coroutine dispatcher.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun insertSettlementMethodsIntoTable(
        offerID: String,
        chainID: String,
        settlementMethods: List<Pair<String, String?>>,
        deletionLambda: (String, String) -> Unit,
        insertionLambda: (String, String, String, String?, String?) -> Unit
    ) {
        withContext(databaseServiceContext) {
            deletionLambda(offerID, chainID)
            for (settlementMethod in settlementMethods) {
                val encryptedPrivateData = encryptPrivateSwapSettlementMethodData(
                    privateSettlementMethodData = settlementMethod.second
                )
                insertionLambda(offerID, chainID, settlementMethod.first, encryptedPrivateData.first,
                    encryptedPrivateData.second)
            }
        }
    }

    /**
     * Removes every persistently stored settlement method associated with an offer ID equal to [offerID] and a chain ID
     * equal to [chainID] from a database table of settlement methods via [deletionLambda].
     *
     * @param offerID The ID of the offer or swap for which associated settlement methods should be removed.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     * @param deletionLambda A lambda that can be executed to delete all settlement methods with a given offer ID and
     * chain ID from the same database table containing settlement methods. This lambda will be executed on the
     * [databaseServiceContext] coroutine dispatcher.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun deleteSettlementMethodsFromTable(
        offerID: String,
        chainID: String,
        deletionLambda: (String, String) -> Unit
    ) {
        withContext(databaseServiceContext) {
            deletionLambda(offerID, chainID)
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
     * Decrypts [privateSettlementMethodData] with [databaseKey] and [privateSettlementMethodDataInitializationVector]
     * and then returns the result as a UTF-8 string, or returns `null` if [privateSettlementMethodData] or
     * [privateSettlementMethodDataInitializationVector] is `null` or if decryption is unsuccessful.
     *
     * @param privateSettlementMethodData The private data to be decrypted, as a Base64 encoded string of bytes.
     * @param privateSettlementMethodDataInitializationVector The initialization vector that will be used to decrypt
     * [privateSettlementMethodData], as a Base64 encoded string of bytes.
     *
     * @return An optional string that will either be a UTF-8 string made from the results of the decryption, or `null`
     * if either of the parameters are `null` or if decryption is unsuccessful.
     */
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

    /**
     * Attempts to decrypt the supplied cipher data using [databaseKey] and the supplied initialization vector string.
     *
     * @param privateDataCipherString The cipher data to be decrypted, as a Base64-encoded string.
     * @param privateDataInitializationVectorString The initialization vector with which to decrypt
     * [privateDataCipherString].
     * @param decryptionFailureHandler A closure that will be executed if decryption fails.
     * @param decodingFailureHandler A closure that will be executed if decoding the cipher string or initialization
     * vector fails.
     *
     * @return [privateDataCipherString] as a decrypted string, or `null` if the decoding and decryption process fails.
     */
    private fun decryptSettlementMethodFromTable(
        privateDataCipherString: String,
        privateDataInitializationVectorString: String,
        decryptionFailureHandler: () -> Unit,
        decodingFailureHandler: () -> Unit
    ): String? {
        val decoder = Base64.getDecoder()
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
        return if (privateCipherData != null && privateDataInitializationVector != null) {
            val privateDataObject = SymmetricallyEncryptedData(
                data = privateCipherData,
                iv = privateDataInitializationVector
            )
            try {
                databaseKey.decrypt(privateDataObject).decodeToString()
            } catch (exception: Exception) {
                decryptionFailureHandler.invoke()
                null
            }
        } else {
            decodingFailureHandler.invoke()
            null
        }
    }

    /**
     * Retrieves and decrupts the persistently stored settlement methods their private data (if any) associated with the
     * specified offer ID and chain ID from a database table containing settlement methods via [selectionLambda], or
     * returns `null` if no such settlement methods are present.
     *
     * @param offerID The ID of the offer for which settlement methods should be returned, as a Base64-[String] of
     * bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     * @param selectionLambda A lambda that can be executed to select all settlement methods with a given offer ID and
     * chain ID from a database table containing settlement methods. This lambda will be executed on the
     * [databaseServiceContext] coroutine dispatcher.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun getAndDecryptSettlementMethodsFromTable(
        offerID: String,
        chainID: String,
        selectionLambda: (String, String) -> List<OfferSettlementMethod>
    ): List<Pair<String, String?>>? {
        val dbSettlementMethods: List<OfferSettlementMethod> = withContext(databaseServiceContext) {
            selectionLambda(offerID, chainID)
        }
        return if (dbSettlementMethods.isNotEmpty()) {
            val settlementMethodsList = mutableListOf<Pair<String, String?>>()
            dbSettlementMethods.forEach {
                var decryptedPrivateDataString: String? = null
                val privateDataCipherString = it.privateData
                val privateDataInitializationVectorString = it.privateDataInitializationVector
                if (privateDataCipherString != null && privateDataInitializationVectorString != null) {
                    decryptedPrivateDataString = decryptSettlementMethodFromTable(
                        privateDataCipherString = privateDataCipherString,
                        privateDataInitializationVectorString = privateDataInitializationVectorString,
                        decryptionFailureHandler = {
                            logger.error("getAndDecryptSettlementMethodsFromTable: unable to get string from " +
                                    "decoded private data using utf8 encoding for offer with B64 ID $offerID")
                        },
                        decodingFailureHandler = {
                            logger.error("getAndDecryptSettlementMethodsFromTable: found private data for " +
                                    "offer with B64 ID $offerID but could not decode bytes from strings")
                        }
                    )
                } else {
                    logger.error("getAndDecryptSettlementMethodsFromTable: did not find private settlement " +
                            "method data for settlement method for offer with B64 ID $offerID")
                }
                settlementMethodsList.add(Pair(it.settlementMethod, decryptedPrivateDataString))
            }
            logger.info("getAndDecryptSettlementMethodsFromTable: returning ${settlementMethodsList.size} for " +
                    "offer with B64 ID $offerID")
            return settlementMethodsList
        } else {
            logger.info("getAndDecryptSettlementMethodsFromTable: none found for offer with B64 ID $offerID")
            null
        }
    }

    /**
     * Calls [DatabaseService.insertSettlementMethodsIntoTable], passing all parameters passed to this function and a
     * lambda that performs insertion into the database table of offers' settlement methods.
     *
     * @param offerID The ID of the offer or swap to be associated with the settlement methods.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     * @param settlementMethods The settlement methods to be persistently stored, as [Pair]s containing a string and an
     * optional string, in that order. The first element in a [Pair] is the public settlement method data, including
     * price, currency and type. The second element in a [Pair] is private data (such as an address or bank account
     * number) for the settlement method, if any.
     *
     * @throws Exception if database insertion is unsuccessful.
     */
    suspend fun storeOfferSettlementMethods(
        offerID: String,
        chainID: String,
        settlementMethods: List<Pair<String, String?>>
    ) {
        insertSettlementMethodsIntoTable(
            offerID = offerID,
            chainID = chainID,
            settlementMethods = settlementMethods,
            deletionLambda = { _offerID, _chainID ->
                database.deleteOfferSettlementMethods(_offerID, _chainID)
            },
            insertionLambda = { _offerID, _chainID, publicData, encryptedPrivateData, initializationVector ->
                database.insertOfferSettlementMethod(
                    OfferSettlementMethod(
                        _offerID,
                        _chainID,
                        publicData,
                        encryptedPrivateData,
                        initializationVector
                    )
                )
            }
        )
        logger.info( "storeOfferSettlementMethods: stored ${settlementMethods.size} for offer with B64 ID $offerID")
    }

    /**
     * Calls [DatabaseService.deleteSettlementMethodsFromTable], passing all parameters passed to this function and a
     * lambda that performs deletion on the database table of offers' current settlement methods.
     *
     * @param offerID The ID of the offer for which associated settlement methods should be removed, as a
     * Base64-[String] of bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or `Swap` corresponding to these settlement methods
     * exists, as a [String].
     */
    suspend fun deleteOfferSettlementMethods(offerID: String, chainID: String) {
        deleteSettlementMethodsFromTable(
            offerID = offerID,
            chainID = chainID,
            deletionLambda = { _offerID, _chainID ->
                database.deleteOfferSettlementMethods(offerID = _offerID, chainID = _chainID)
            }
        )
        logger.info("deleteOfferSettlementMethods: deleted for offer with B64 ID $offerID")
    }

    /**
     * Calls [DatabaseService.getAndDecryptSettlementMethodsFromTable], passing all parameters passed to this function
     * and a table that performs selection on the database table containing offers' current settlement methods.
     *
     * @param offerID The ID of the offer for which settlement methods should be returned, as a Base64-[String] of
     * bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     *
     * @return The value returned by [DatabaseService.getAndDecryptSettlementMethodsFromTable].
     */
    suspend fun getOfferSettlementMethods(offerID: String, chainID: String): List<Pair<String, String?>>? {
        logger.info("getOfferSettlementMethods: getting for offer with B64 ID $offerID")
        return getAndDecryptSettlementMethodsFromTable(
            offerID = offerID,
            chainID = chainID,
            selectionLambda = { _offerID, _chainID ->
                database.selectOfferSettlementMethodByOfferIdAndChainID(_offerID, _chainID)
            }
        )
    }

    /**
     * Calls [DatabaseService.insertSettlementMethodsIntoTable], passing all parameters passed to this function and a
     * lambda that performs insertion into the database table of offers' pending settlement methods.
     *
     * @param offerID The ID of the offer or swap to be associated with the pending settlement methods.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these settlement methods
     * exists, as a [String].
     * @param pendingSettlementMethods The pending settlement methods to be persistently stored, as pairs containing a
     * string and an optional string, in that order. The first element in each pair is the public settlement method
     * data, including price, currency and type. The second element element in each pair is private data (such as an
     * address or bank account number) for the settlement method, if any.
     */
    suspend fun storePendingOfferSettlementMethods(
        offerID: String,
        chainID: String,
        pendingSettlementMethods: List<Pair<String, String?>>
    ) {
        insertSettlementMethodsIntoTable(
            offerID = offerID,
            chainID = chainID,
            settlementMethods = pendingSettlementMethods,
            deletionLambda = { _offerID, _chainID ->
                database.deletePendingOfferSettlementMethods(_offerID, _chainID)
            },
            insertionLambda = { _offerID, _chainID, publicData, encryptedPrivateData, initializationVector ->
                database.insertPendingOfferSettlementMethod(
                    OfferSettlementMethod(
                        _offerID,
                        _chainID,
                        publicData,
                        encryptedPrivateData,
                        initializationVector
                    )
                )
            }
        )
        logger.info("storePendingOfferSettlementMethods: stored ${pendingSettlementMethods.size} for offer with " +
                "B64 ID $offerID")
    }

    /**
     * Calls [DatabaseService.deleteSettlementMethodsFromTable], passing all parameters passed to this function and a
     * lambda that performs deletion on the database table containing pending settlement methods.
     *
     * @param offerID The ID of the offer for which associated pending settlement methods should be removed.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these pending settlement
     * methods exists, as a [String].
     */
    suspend fun deletePendingOfferSettlementMethods(offerID: String, chainID: String) {
        deleteSettlementMethodsFromTable(
            offerID = offerID,
            chainID = chainID,
            deletionLambda = { _offerID, _chainID ->
                database.deletePendingOfferSettlementMethods(offerID = _offerID, chainID = _chainID)
            }
        )
        logger.info("deletePendingOfferSettlementMethods: deleted for offer with B64 ID $offerID")
    }

    /**
     * Calls [DatabaseService.getAndDecryptSettlementMethodsFromTable], passing all parameters passed to this function
     * and a lambda that performs selection on the database table of offers' pending settlement methods.
     *
     * @param offerID The ID of the offer for which pending settlement methods should be returned, as a Base64-[String]
     * of bytes.
     * @param chainID The ID of the blockchain on which the [Offer] or [Swap] corresponding to these pending settlement
     * methods exists, as a [String].
     */
    suspend fun getPendingOfferSettlementMethods(offerID: String, chainID: String): List<Pair<String, String?>>? {
        logger.info("getPendingOfferSettlementMethods: getting for offer with B64 ID $offerID")
        return getAndDecryptSettlementMethodsFromTable(
            offerID = offerID,
            chainID = chainID,
            selectionLambda = { _offerID, _chainID ->
                database.selectPendingOfferSettlementMethodByOfferIdAndChainID(_offerID, _chainID)
            }
        )
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
            role = swap.role,
            reportPaymentSentState = swap.reportPaymentSentState,
            reportPaymentSentTransactionHash = swap.reportPaymentSentTransactionHash,
            reportPaymentSentTransactionCreationTime = swap.reportPaymentSentTransactionCreationTime,
            reportPaymentSentTransactionCreationBlockNumber = swap.reportPaymentSentTransactionCreationBlockNumber,
            reportPaymentReceivedState = swap.reportPaymentReceivedState,
            reportPaymentReceivedTransactionHash = swap.reportPaymentReceivedTransactionHash,
            reportPaymentReceivedTransactionCreationTime = swap.reportPaymentReceivedTransactionCreationTime,
            reportPaymentReceivedTransactionCreationBlockNumber = swap
                .reportPaymentReceivedTransactionCreationBlockNumber,
            closeSwapState = swap.closeSwapState,
            closeSwapTransactionHash = swap.closeSwapTransactionHash,
            closeSwapTransactionCreationTime = swap.closeSwapTransactionCreationTime,
            closeSwapTransactionCreationBlockNumber = swap.closeSwapTransactionCreationBlockNumber,
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
     * Updates a persistently stored [Swap]'s [Swap.makerPrivateData] and [Swap.makerPrivateDataInitializationVector]
     * fields with the results of encrypting [data].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The chain ID of the swap to be updated, as a [String].
     * @param data New private settlement method data belonging to the maker of the swap with the specified ID and chain
     * ID, to be encrypted and stored.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapMakerPrivateSettlementMethodData(swapID: String, chainID: String, data: String?) {
        val encryptedData = encryptPrivateSwapSettlementMethodData(privateSettlementMethodData = data)
        withContext(databaseServiceContext) {
            database.updateSwapMakerPrivateSettlementMethodData(
                swapID,
                chainID,
                encryptedData.first,
                encryptedData.second
            )
        }
        logger.info("updateSwapMakerPrivateSettlementMethodData: updated for $swapID, if present")
    }

    /**
     * Updates a persistently stored [Swap]'s [Swap.takerPrivateData] and [Swap.takerPrivateDataInitializationVector]
     * fields with the results of encrypting [data].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The chain ID of the swap to be updated, as a [String].
     * @param data New private settlement method data belonging to the taker of the swap with the specified ID and chain
     * ID, to be encrypted and stored.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateSwapTakerPrivateSettlementMethodData(swapID: String, chainID: String, data: String?) {
        val encryptedData = encryptPrivateSwapSettlementMethodData(privateSettlementMethodData = data)
        withContext(databaseServiceContext) {
            database.updateSwapTakerPrivateSettlementMethodData(
                swapID,
                chainID,
                encryptedData.first,
                encryptedData.second
            )
        }
        logger.info("updateSwapTakerPrivateSettlementMethodData: updated for $swapID, if present")
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
     * Updates the [Swap.reportPaymentSentState] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param state The new value of the swap's [Swap.reportPaymentSentState] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateReportPaymentSentState(swapID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateReportPaymentSentState(swapID, chainID, state)
        }
        logger.info("updateReportPaymentSentState: set value to $state for offer with B64 ID $swapID, if present")
    }

    /**
     * Updates the [Swap.reportPaymentSentTransactionHash], [Swap.reportPaymentSentTransactionCreationTime] and
     * [Swap.reportPaymentSentTransactionCreationBlockNumber] properties of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param transactionHash The new value of the swap's [Swap.reportPaymentSentTransactionHash] property, as a
     * transaction hash as a hexadecimal string with "0x" prefix.
     * @param creationTime The new value of the swap's [Swap.reportPaymentSentTransactionCreationTime] property.
     * @param blockNumber The new value of the swap's [Swap.reportPaymentSentTransactionCreationBlockNumber] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateReportPaymentSentData(
        swapID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        withContext(databaseServiceContext) {
            database.updateReportPaymentSentData(
                swapID = swapID,
                chainID = chainID,
                transactionHash = transactionHash,
                creationTime = creationTime,
                blockNumber = blockNumber,
            )
        }
        logger.info("updateReportPaymentSentData: set values to $transactionHash, $creationTime and $blockNumber for " +
                "offer with B64 ID $swapID, if present")
    }

    /**
     * Updates the [Swap.reportPaymentReceivedState] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param state The new value of the swap's [Swap.reportPaymentSentState] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateReportPaymentReceivedState(swapID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateReportPaymentReceivedState(swapID, chainID, state)
        }
        logger.info("updateReportPaymentReceivedState: set value to $state for offer with B64 ID $swapID, if " +
                "present")
    }

    /**
     * Updates the [Swap.reportPaymentReceivedTransactionHash], [Swap.reportPaymentReceivedTransactionCreationTime] and
     * [Swap.reportPaymentReceivedTransactionCreationBlockNumber] properties of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param transactionHash The new value of the swap's [Swap.reportPaymentReceivedTransactionHash] property, as a
     * transaction hash as a hexadecimal string with "0x" prefix.
     * @param creationTime The new value of the swap's [Swap.reportPaymentReceivedTransactionCreationTime] property.
     * @param blockNumber The new value of the swap's [Swap.reportPaymentReceivedTransactionCreationBlockNumber]
     * property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateReportPaymentReceivedData(
        swapID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        withContext(databaseServiceContext) {
            database.updateReportPaymentReceivedData(
                swapID = swapID,
                chainID = chainID,
                transactionHash = transactionHash,
                creationTime = creationTime,
                blockNumber = blockNumber,
            )
        }
        logger.info("updateReportPaymentReceivedData: set values to $transactionHash, $creationTime and " +
                "$blockNumber for offer with B64 ID $swapID, if present")
    }

    /**
     * Updates the [Swap.closeSwapState] property of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param state The new value of the swap's [Swap.closeSwapState] property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateCloseSwapState(swapID: String, chainID: String, state: String) {
        withContext(databaseServiceContext) {
            database.updateCloseSwapState(swapID, chainID, state)
        }
        logger.info("updateCloseSwapState: set value to $state for offer with B64 ID $swapID, if present")
    }

    /**
     * Updates the [Swap.closeSwapTransactionHash], [Swap.closeSwapTransactionCreationTime] and
     * [Swap.closeSwapTransactionCreationBlockNumber] properties of a persistently stored
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) with the specified [swapID] and
     * [chainID].
     *
     * @param swapID The ID of the swap to be updated, as a Base64-[String] of bytes.
     * @param chainID The blockchain ID of the swap to be updated, as a [String].
     * @param transactionHash The new value of the swap's [Swap.closeSwapTransactionHash] property, as a
     * transaction hash as a hexadecimal string with "0x" prefix.
     * @param creationTime The new value of the swap's [Swap.closeSwapTransactionCreationTime] property.
     * @param blockNumber The new value of the swap's [Swap.closeSwapTransactionCreationBlockNumber]
     * property.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateCloseSwapData(
        swapID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        withContext(databaseServiceContext) {
            database.updateCloseSwapData(
                swapID = swapID,
                chainID = chainID,
                transactionHash = transactionHash,
                creationTime = creationTime,
                blockNumber = blockNumber,
            )
        }
        logger.info("updateCloseSwapData: set values to $transactionHash, $creationTime and $blockNumber for offer " +
                "with B64 ID $swapID, if present")
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
                reportPaymentSentState = dbSwaps[0].reportPaymentSentState,
                reportPaymentSentTransactionHash = dbSwaps[0].reportPaymentSentTransactionHash,
                reportPaymentSentTransactionCreationTime = dbSwaps[0].reportPaymentSentTransactionCreationTime,
                reportPaymentSentTransactionCreationBlockNumber = dbSwaps[0]
                    .reportPaymentSentTransactionCreationBlockNumber,
                reportPaymentReceivedState = dbSwaps[0].reportPaymentReceivedState,
                reportPaymentReceivedTransactionHash = dbSwaps[0].reportPaymentReceivedTransactionHash,
                reportPaymentReceivedTransactionCreationTime = dbSwaps[0].reportPaymentReceivedTransactionCreationTime,
                reportPaymentReceivedTransactionCreationBlockNumber = dbSwaps[0]
                    .reportPaymentReceivedTransactionCreationBlockNumber,
                closeSwapState = dbSwaps[0].closeSwapState,
                closeSwapTransactionHash = dbSwaps[0].closeSwapTransactionHash,
                closeSwapTransactionCreationTime = dbSwaps[0].closeSwapTransactionCreationTime,
                closeSwapTransactionCreationBlockNumber = dbSwaps[0]
                    .closeSwapTransactionCreationBlockNumber,
            )
        } else {
            logger.info("getSwap: no swap found with B64 ID $id")
            null
        }
    }

    /**
     * Persistently stores the settlement method string and private data string, associating them with the given UUID
     * string. The private settlement method data is encrypted with [databaseKey] and a new initialization vector.
     *
     * @param id The ID of the settlement method to be stored, as a Type 4 UUID string.
     * @param settlementMethod The public data of the settlement method to be persistently stored, including currency
     * and type.
     * @param privateData The private data of the settlement method, such as an address or bank account number for the
     * settlement method, if any.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun storeUserSettlementMethod(
        id: String,
        settlementMethod: String,
        privateData: String?
    ) {
        val encryptedPrivateData = encryptPrivateSwapSettlementMethodData(privateSettlementMethodData = privateData)
        withContext(databaseServiceContext) {
            database.insertUserSettlementMethod(
                UserSettlementMethod(
                    settlementMethodID = id,
                    settlementMethod = settlementMethod,
                    privateData = encryptedPrivateData.first,
                    privateDataInitializationVector = encryptedPrivateData.second
                )
            )
        }
        logger.info("storeUserSettlementMethod: stored $id")
    }

    /**
     * Updates the private settlement method data of a persistently stored settlement method with the given ID. The new
     * private settlement method data is encrypted with [databaseKey] and a new initialization vector.
     *
     * @param id The ID of the settlement method, the private data of which this will update.
     * @param privateData The new private settlement method data with which this will replace the current private
     * settlement method data of the settlement method with the ID [id].
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun updateUserSettlementMethod(
        id: String,
        privateData: String?
    ) {
        logger.info("updateUserSettlementMethod: updating $id")
        val encryptedPrivateData = encryptPrivateSwapSettlementMethodData(privateSettlementMethodData = privateData)
        withContext(databaseServiceContext) {
            database.updateUserSettlementMethod(id, encryptedPrivateData.first, encryptedPrivateData.second)
        }
    }

    /**
     * Removes every persistently stored settlement method with an ID equal to [id] from the table of the user's
     * settlement methods.
     *
     * @param id The ID of the settlement method(s) to be deleted.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun deleteUserSettlementMethod(
        id: String,
    ) {
        logger.info("deleteUserSettlementMethod: deleting $id")
        withContext(databaseServiceContext) {
            database.deleteUserSettlementMethod(id)
        }
    }

    /**
     * Retrieves and decrypts the persistently stored settlement method and its private data (if any) associated with
     * the specified settlement method ID from the table of the user's settlement methods, or returns `nil` if no such
     * settlement method is found.
     *
     * @param id The ID of the settlement method to be retrieved, as a Type 4 UUID string.
     *
     * @return `null` if no such settlement method is found, or a [Pair] containing a string and an optional string, the
     * first of which is the settlement method associated with [id], the second of which is the private data associated
     * with that settlement method, or `null` if no such data is found or if it cannot be decrypted.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getUserSettlementMethod(id: String): Pair<String, String?>? {
        logger.info("getUserSettlementMethod: getting $id")
        val dbSettlementMethods: List<UserSettlementMethod> = withContext(databaseServiceContext) {
            database.selectUserSettlementMethodByID(
                id = id
            )
        }
        if (dbSettlementMethods.size == 1) {
            val element = dbSettlementMethods.first()
            var decryptedPrivateDataString: String? = null
            val privateDataCipherString = element.privateData
            val privateDataInitializationVectorString = element.privateDataInitializationVector
            if (privateDataCipherString != null && privateDataInitializationVectorString != null) {
                decryptedPrivateDataString = decryptSettlementMethodFromTable(
                    privateDataCipherString = privateDataCipherString,
                    privateDataInitializationVectorString = privateDataInitializationVectorString,
                    decryptionFailureHandler = {
                        logger.error("getUserSettlementMethod: unable to get string from decoded private data " +
                                "using utf8 encoding for $id")
                    },
                    decodingFailureHandler = {
                        logger.error("getUserSettlementMethod: unable to get string from decoded private data " +
                                "using utf8 encoding for $id")
                    }
                )
            } else {
                logger.info("getUserSettlementMethod: did not find private settlement method data for $id")
            }
            return Pair(element.settlementMethod, decryptedPrivateDataString)
        } else {
            logger.info("getUserSettlementMethod: $id not found")
            return null
        }
    }

}