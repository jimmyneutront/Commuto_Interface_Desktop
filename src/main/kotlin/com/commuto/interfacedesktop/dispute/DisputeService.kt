package com.commuto.interfacedesktop.dispute

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionType
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.dispute.validation.validateSwapForRaisingDispute
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.swap.Swap
import com.commuto.interfacedesktop.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Numeric
import java.util.*

/**
 * The main Dispute service. It is responsible for processing and organizing swap-related data.
 *
 * @property databaseService The [DatabaseService] that this [DisputeService] uses for persistent storage.
 * @property blockchainService The [BlockchainService] that this uses to interact with the blockchain.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 */
class DisputeService constructor(
    private val databaseService: DatabaseService,
){

    private lateinit var blockchainService: BlockchainService

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Used to set the [blockchainService] property. This can only be called once.
     *
     * @param newBlockchainService The new value of the [blockchainService] property, which cannot be null.
     */
    fun setBlockchainService(newBlockchainService: BlockchainService) {
        check(!::blockchainService.isInitialized) {
            "blockchainService is already initialized"
        }
        blockchainService = newBlockchainService
    }

    /**
     * Attempts to create a [RawTransaction] that will raise a dispute for a
     * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap) involving the user of this
     * interface.
     *
     * This calls [validateSwapForRaisingDispute], then gets all active dispute agent addresses, randomly selects three
     * of them, and then passes the selected addresses along with the ID and chain ID of [swap] to
     * [BlockchainService.createRaiseDisputeTransaction].
     *
     * @param swap The [Swap] for which a dispute will be raised.
     *
     * @return A [Pair], the first element of which is a [RawTransaction] capable of raising a dispute for [swap], and
     * the second element of which is a [Triple] of [String]s that are the selected dispute agents.
     */
    suspend fun createRaiseDisputeTransaction(swap: Swap): Pair<RawTransaction, Triple<String, String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("createRaiseDisputeTransaction: creating for ${swap.id}")
                validateSwapForRaisingDispute(swap = swap)
                val activeDisputeAgents = blockchainService.getActiveDisputeAgentsAsync().await()
                val disputeAgentZero = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentZero)
                val disputeAgentOne = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentOne)
                val disputeAgentTwo = activeDisputeAgents.random() as String
                activeDisputeAgents.remove(disputeAgentTwo)
                Pair(
                    blockchainService.createRaiseDisputeTransaction(
                        swapID = swap.id,
                        chainID = swap.chainID,
                        disputeAgent0 = disputeAgentZero,
                        disputeAgent1 = disputeAgentOne,
                        disputeAgent2 = disputeAgentTwo,
                    ),
                    Triple(
                        disputeAgentZero,
                        disputeAgentOne,
                        disputeAgentTwo
                    )
                )
            } catch (exception: Exception) {
                logger.error("createRaiseDisputeTransaction: encountered exception while creating for ${swap.id}", exception)
                throw exception
            }
        }
    }

    /**
     * Attempts to raise a dispute for a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap)
     * involving the user of this interface.
     *
     * @param swap The [Swap] for which to raise a dispute.
     * @param disputeRaisingTransaction An optional [RawTransaction] that can raise a dispute for [swap].
     * @param disputeAgents An optional [Triple] of [String]s, containing the addresses of the dispute agents with which
     * [disputeRaisingTransaction] was created.
     *
     * @throws [DisputeServiceException] if [disputeAgents] is `null` or if [disputeRaisingTransaction] is `null` or if
     * the data of [disputeRaisingTransaction] does not match that of the transaction this function creates using [swap]
     * and [disputeAgents].
     */
    suspend fun raiseDispute(
        swap: Swap,
        disputeRaisingTransaction: RawTransaction?,
        disputeAgents: Triple<String, String, String>?
    ) {
        withContext(Dispatchers.IO) {
            val encoder = Base64.getEncoder()
            try  {
                logger.info("raiseDispute: raising for ${swap.id}")
                validateSwapForRaisingDispute(swap = swap)
                if (disputeAgents == null) {
                    throw DisputeServiceException(message = "disputeAgents was null during raiseDispute call for " +
                            "${swap.id}")
                }
                logger.info("raiseDispute: recreating RawTransaction to dispute ${swap.id} to ensure " +
                        "disputeRaisingTransaction was created with the contents of swap")
                val recreatedTransaction = blockchainService.createRaiseDisputeTransaction(
                    swapID = swap.id,
                    chainID = swap.chainID,
                    disputeAgent0 = disputeAgents.first,
                    disputeAgent1 = disputeAgents.second,
                    disputeAgent2 = disputeAgents.third
                )
                if (disputeRaisingTransaction == null) {
                    throw DisputeServiceException(message = "Transaction was null during raiseDispute call for " +
                            "${swap.id}")
                }
                if (recreatedTransaction.data != disputeRaisingTransaction.data) {
                    throw DisputeServiceException(message = "Data for disputeRaisingTransaction did not match that of " +
                            "transaction created with swap ${swap.id}")
                }
                logger.info("raiseDispute: signing transaction for ${swap.id}")
                val signedTransactionData = blockchainService.signTransaction(
                    transaction = disputeRaisingTransaction,
                    chainID = swap.chainID,
                )
                val signedTransactionHex = Numeric.toHexString(signedTransactionData)
                val blockchainTransactionForRaisingDispute = BlockchainTransaction(
                    transaction = disputeRaisingTransaction,
                    transactionHash = Hash.sha3(signedTransactionHex),
                    latestBlockNumberAtCreation = blockchainService.newestBlockNum,
                    type = BlockchainTransactionType.RAISE_DISPUTE
                )
                val dateString = DateFormatter.createDateString(blockchainTransactionForRaisingDispute.timeOfCreation)
                logger.info("raiseDispute: persistently storing dispute raising data for ${swap.id}, including tx " +
                        "hash ${blockchainTransactionForRaisingDispute.transactionHash}")
                databaseService.updateRaisingDisputeData(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    transactionHash = blockchainTransactionForRaisingDispute.transactionHash,
                    creationTime = dateString,
                    blockNumber = blockchainTransactionForRaisingDispute.latestBlockNumberAtCreation.toLong()
                )
                logger.info("raiseDispute: persistently  updating raisingDisputeState for ${swap.id} to " +
                        "${RaisingDisputeState.SENDING_TRANSACTION}")
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.SENDING_TRANSACTION.asString,
                )
                logger.info("raiseDispute: updating raisingDisputeState to ${RaisingDisputeState.SENDING_TRANSACTION} " +
                        "and storing tx ${blockchainTransactionForRaisingDispute.transactionHash} in ${swap.id}")
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeTransaction = blockchainTransactionForRaisingDispute
                    swap.raisingDisputeState.value = RaisingDisputeState.SENDING_TRANSACTION
                }
                logger.info("raiseDispute: sending ${blockchainTransactionForRaisingDispute.transactionHash} for " +
                        "${swap.id}")
                blockchainService.sendTransaction(
                    transaction = blockchainTransactionForRaisingDispute,
                    signedRawTransactionDataAsHex = signedTransactionHex,
                    chainID = swap.chainID
                )
                logger.info("raiseDispute: persistently updating raisingDisputeState of ${swap.id} to " +
                        "${RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION}")
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION.asString,
                )
                logger.info("raiseDispute: updating raisingDisputeState to " +
                        "${RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION} for ${swap.id}")
                withContext(Dispatchers.Main){
                    swap.raisingDisputeState.value = RaisingDisputeState.AWAITING_TRANSACTION_CONFIRMATION
                }
            } catch (exception: Exception) {
                logger.error("raiseDispute: encountered exception while raising dispute for ${swap.id}, setting " +
                        "raisingDisputeState to ${RaisingDisputeState.EXCEPTION}", exception)
                databaseService.updateRaisingDisputeState(
                    swapID = encoder.encodeToString(swap.id.asByteArray()),
                    chainID = swap.chainID.toString(),
                    state = RaisingDisputeState.EXCEPTION.asString,
                )
                withContext(Dispatchers.Main) {
                    swap.raisingDisputeException = exception
                    swap.raisingDisputeState.value = RaisingDisputeState.EXCEPTION
                }
            }
        }
    }

}