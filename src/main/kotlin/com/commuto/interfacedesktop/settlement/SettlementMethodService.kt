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
     * On the IO coroutine dispatcher, this ensures that the supplied private data corresponds to the type of settlement
     * method that the user is adding. Then this serializes the supplied [PrivateData] accordingly, and associates it
     * with the supplied [SettlementMethod]. Then this securely stores the new [SettlementMethod] and associated
     * serialized private data in persistent storage, via [databaseService]. Finally, on the main coroutine dispatcher,
     * this appends the new [SettlementMethod] to the list of settlement methods in [settlementMethodTruthSource].
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
            logger.info("addSettlementMethod: adding ${settlementMethod.currency} via " +
                    settlementMethod.method
            )
            try {
                try {
                    when (settlementMethod.method) {
                        "SEPA" -> {
                            settlementMethod.privateData = Json.encodeToString(newPrivateData as PrivateSEPAData)
                        }
                        "SWIFT" -> {
                            settlementMethod.privateData = Json.encodeToString(newPrivateData as PrivateSWIFTData)
                        }
                        else -> {
                            throw SettlementMethodServiceException("Did not recognize settlement method")
                        }
                    }
                    afterSerialization?.invoke()
                } catch (exception: Exception) {
                    logger.error("addSettlementMethod: got exception while adding ${settlementMethod.currency} via " +
                            settlementMethod.method, exception)
                    throw exception
                }
                logger.info("addSettlementMethod: persistently storing settlement method " +
                        settlementMethod.currency
                )
                // TODO: persistently store settlement method here
                afterPersistentStorage?.invoke()
                logger.info("addSettlementMethod: adding ${settlementMethod.currency} via " +
                        "${settlementMethod.method} to settlementMethodTruthSource")
                withContext(Dispatchers.Main) {
                    settlementMethodTruthSource.settlementMethods.add(settlementMethod)
                }
            } catch (exception: Exception) {
                logger.error("addSettlementMethod: encountered exception", exception)
                throw exception
            }
        }
    }

}