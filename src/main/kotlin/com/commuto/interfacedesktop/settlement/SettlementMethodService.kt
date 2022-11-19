package com.commuto.interfacedesktop.settlement

import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import com.commuto.interfacedesktop.ui.settlement.UISettlementMethodTruthSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Settlement Method Service. It is responsible for validating, adding, editing, and removing the user's
 * settlement methods and their corresponding private data, both in persistent storage and in a
 * [UISettlementMethodTruthSource].
 *
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property databaseService The [DatabaseService] used for persistent storage.
 */
@Singleton
class SettlementMethodService @Inject constructor(
    private val databaseService: DatabaseService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var settlementMethodTruthSource: UISettlementMethodTruthSource

    /**
     * Used to set the [settlementMethodTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [settlementMethodTruthSource] property, which cannot be null.
     */
    fun setSettlementMethodTruthSource(newTruthSource: UISettlementMethodTruthSource) {
        check(!::settlementMethodTruthSource.isInitialized) {
            "settlementMethodTruthSource is already initialized"
        }
        settlementMethodTruthSource = newTruthSource
    }

    /**
     * Attempts to add a [SettlementMethod] with corresponding [PrivateData] to the list of the user's settlement
     * methods.
     *
     * On the IO coroutine dispatcher, this validates and serializes the supplied private data via
     * [serializePrivateSettlementMethodData]. Then this serializes the supplied [PrivateData] accordingly, and
     * associates it with the supplied [SettlementMethod]. Then this securely stores the new [SettlementMethod] and
     * associated serialized private data in persistent storage, via [databaseService]. Finally, on the main coroutine
     * dispatcher, this appends the new [SettlementMethod] to the list of settlement methods in
     * [settlementMethodTruthSource].
     *
     * @param settlementMethod The [SettlementMethod] to be added.
     * @param newPrivateData The [PrivateData] for the new settlement method.
     * @param afterSerialization A closure that will be executed after [newPrivateData] has been serialized.
     * @param afterPersistentStorage A closure that will be executed after [settlementMethod] is saved in persistent
     * storage.
     *
     * @throws SettlementMethodServiceException if [settlementMethod] is of an unknown type.
     */
    suspend fun addSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        afterSerialization: (suspend () -> Unit)? = null,
        afterPersistentStorage: (suspend () -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            logger.info("addSettlementMethod: adding ${settlementMethod.id}")
            try {
                val settlementMethodWithPrivateData = try {
                    val settlementMethodWithPrivateData = serializePrivateSettlementMethodData(
                        settlementMethod = settlementMethod,
                        privateData = newPrivateData
                    )
                    afterSerialization?.invoke()
                    settlementMethodWithPrivateData
                } catch (exception: Exception) {
                    logger.error("addSettlementMethod: got exception while adding ${settlementMethod.id}", exception)
                    throw exception
                }
                logger.info("addSettlementMethod: persistently storing settlement method ${settlementMethod.id}")
                databaseService.storeUserSettlementMethod(
                    id = settlementMethod.id.toString(),
                    settlementMethod = Json.encodeToString(settlementMethodWithPrivateData),
                    privateData = settlementMethodWithPrivateData.privateData
                )
                afterPersistentStorage?.invoke()
                logger.info("addSettlementMethod: adding ${settlementMethod.id} to settlementMethodTruthSource")
                withContext(Dispatchers.Main) {
                    settlementMethodTruthSource.settlementMethods.add(settlementMethod)
                }
            } catch (exception: Exception) {
                logger.error("addSettlementMethod: encountered exception while adding ${settlementMethod.id}",
                    exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to edit the private data of the given [SettlementMethod] in the list of the user's settlement methods,
     * both in [settlementMethodTruthSource] and in persistent storage.
     *
     * On the IO coroutine dispatcher, this validates and serializes the supplied private data via
     * [serializePrivateSettlementMethodData]. Then this updates the private data of the settlement method in persistent
     * storage with this newly serialized private data. Finally, on the main coroutine dispatcher, this finds the
     * [SettlementMethod] in [settlementMethodTruthSource] with an id equal to that of [settlementMethod], and sets its
     * [SettlementMethod.privateData] property equal to the newly serialized data.
     *
     * @param settlementMethod The existing [SettlementMethod] to be edited.
     * @param newPrivateData The new private data with which this will update the existing [SettlementMethod].
     * @param afterSerialization A lambda that will be executed after [newPrivateData] has been serialized.
     * @param afterPersistentStorage A lambda that will be executed after this updates the private data of
     * [settlementMethod] in persistent storage.
     */
    suspend fun editSettlementMethod(
        settlementMethod: SettlementMethod,
        newPrivateData: PrivateData,
        afterSerialization: (suspend () -> Unit)? = null,
        afterPersistentStorage: (suspend () -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            logger.info("editSettlementMethod: editing ${settlementMethod.id}")
            try {
                val settlementMethodWithPrivateData = try {
                    val settlementMethodWithPrivateData = serializePrivateSettlementMethodData(
                        settlementMethod = settlementMethod,
                        privateData = newPrivateData
                    )
                    afterSerialization?.invoke()
                    settlementMethodWithPrivateData
                } catch (exception: Exception) {
                    logger.error("editSettlementMethod: got exception while editing ${settlementMethod.id}",
                        exception)
                    throw exception
                }
                logger.info("editSettlementMethod: persistently storing edited private data for " +
                        "${settlementMethod.id}")
                databaseService.updateUserSettlementMethod(
                    id = settlementMethod.id.toString(),
                    privateData = settlementMethodWithPrivateData.privateData
                )
                afterPersistentStorage?.invoke()
                logger.info("editSettlementMethod: editing ${settlementMethod.id} in " +
                        "settlementMethodTruthSource")
                withContext(Dispatchers.Main) {
                    val indexOfEditedSettlementMethod = settlementMethodTruthSource.settlementMethods.indexOfFirst {
                        it.id == settlementMethodWithPrivateData.id
                    }
                    if (indexOfEditedSettlementMethod > -1) {
                        settlementMethodTruthSource.settlementMethods[indexOfEditedSettlementMethod].privateData =
                            settlementMethodWithPrivateData.privateData
                    }
                }
            } catch (exception: Exception) {
                logger.error("editSettlementMethod: encountered exception while editing ${settlementMethod.id}",
                    exception)
                throw exception
            }
        }
    }

    /**
     * Ensures that the given [PrivateData] corresponds to the method in the given [SettlementMethod], serializes
     * [privateData] accordingly, sets the [SettlementMethod.privateData] field of [settlementMethod] equal to the
     * result, and then returns [settlementMethod].
     *
     * @param settlementMethod The [SettlementMethod] for which this should serialize private data.
     * @param privateData The [PrivateData] to be serialized.
     *
     * @return [settlementMethod], the private data property of which is [privateData] serialized to a string.
     *
     * @throws [SettlementMethodServiceException] if [privateData] cannot be cast as the type of private data
     * corresponding to the type specified in [settlementMethod], or if serialization fails.
     */
    private fun serializePrivateSettlementMethodData(
        settlementMethod: SettlementMethod,
        privateData: PrivateData
    ): SettlementMethod {
        when (settlementMethod.method) {
            "SEPA" -> {
                settlementMethod.privateData = Json.encodeToString(privateData as PrivateSEPAData)
            }
            "SWIFT" -> {
                settlementMethod.privateData = Json.encodeToString(privateData as PrivateSWIFTData)
            }
            else -> {
                throw SettlementMethodServiceException("Did not recognize settlement method")
            }
        }
        return settlementMethod
    }

    /**
     * Attempts to delete the given [SettlementMethod] from the list of the user's settlement methods, both in
     * [settlementMethodTruthSource] and in persistent storage.
     *
     * @param settlementMethod The existing [SettlementMethod] that this will delete.
     */
    suspend fun deleteSettlementMethod(
        settlementMethod: SettlementMethod,
    ) {
        withContext(Dispatchers.IO) {
            logger.info("deleteSettlementMethod: removing ${settlementMethod.id} from persistent storage")
            try {
                databaseService.deleteUserSettlementMethod(id = settlementMethod.id.toString())
                logger.info("deleteSettlementmethod: deleting ${settlementMethod.id} from " +
                        "settlementMethodTruthSource")
                withContext(Dispatchers.Main) {
                    settlementMethodTruthSource.settlementMethods.removeIf {
                        it.id == settlementMethod.id
                    }
                }
            } catch (exception: Exception) {
                logger.error("deleteSettlementMethod: encountered exception while deleting " +
                        "${settlementMethod.id}", exception)
                throw exception
            }
        }
    }

}