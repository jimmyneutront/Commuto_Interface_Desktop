package com.commuto.interfacedesktop.blockchain

import org.web3j.crypto.RawTransaction
import java.math.BigInteger
import java.util.Date

/**
 * A wrapper around an optional [RawTransaction] that was created by this interface and should be monitored by this
 * interface for confirmation, transaction dropping, transaction failure and transaction success. The [transaction]
 * property of an instance of this struct may be `null` if this [BlockchainTransaction] represents a transaction that
 * was created before a restart of the application. In this case, only the hash of such a transaction is saved in
 * persistent storage, and so this struct will have been re-created using that hash in persistent storage, not the
 * [RawTransaction] itself. This also contains the date and time at which the original transaction was created, as well
 * as the latest block number of the blockchain at the time when the original transaction was created.
 *
 * @property transaction The [RawTransaction] that this wraps.
 * @property transactionHash The hash of [transaction], or if [transaction] is `null`, the hash of the transaction that
 * this struct represents.
 * @property timeOfCreation The date and time at which this was created.
 * @property latestBlockNumberAtCreation The number of the latest block in the blockchain at the time when this
 * transaction was created.
 * @property type Describes what [transaction] does. (For example: one that opens a new offer, one that fills a swap,
 * etc).
 */
data class BlockchainTransaction(
    val transaction: RawTransaction?,
    val transactionHash: String,
    val timeOfCreation: Date,
    val latestBlockNumberAtCreation: BigInteger,
    val type: BlockchainTransactionType,
) {

    /**
     * Creates a [BlockchainTransaction] wrapped around a [RawTransaction], with a hash equal to [transactionHash], of a
     * specified [BlockchainTransactionType], created when the number of the newest block on the blockchain was
     * [latestBlockNumberAtCreation], and using the current time as [timeOfCreation].
     *
     * @param transaction The [RawTransaction] that this wraps.
     * @param transactionHash The hash of [transaction] (after it has been signed).
     * @param latestBlockNumberAtCreation The number of the newest block on the blockchain at the time when
     * [transaction] was created.
     * @param type A [BlockchainTransactionType] indicating what [transaction] will do when/if it is confirmed.
     */
    constructor(
        transaction: RawTransaction,
        transactionHash: String,
        latestBlockNumberAtCreation: BigInteger,
        type: BlockchainTransactionType
    ): this(
        transaction = transaction,
        transactionHash = if (!transactionHash.startsWith("0x")) {
            "0x${transactionHash.lowercase()}"
        } else {
            transactionHash.lowercase()
        },
        timeOfCreation = Date(),
        latestBlockNumberAtCreation = latestBlockNumberAtCreation,
        type = type
    )

    /**
     * Creates a [BlockchainTransaction] with a `null` [BlockchainTransaction.transaction] property, with a hash equal
     * to [transactionHash],  which identifies a [RawTransaction] created at [timeOfCreation], when the latest block had
     * block number [latestBlockNumberAtCreation], and is of type [type].
     */
    constructor(
        transactionHash: String,
        timeOfCreation: Date,
        latestBlockNumberAtCreation: BigInteger,
        type: BlockchainTransactionType,
    ): this(
        transaction = null,
        transactionHash = transactionHash,
        timeOfCreation = timeOfCreation,
        latestBlockNumberAtCreation = latestBlockNumberAtCreation,
        type = type
    )
}

/**
 * An exception thrown by [BlockchainTransaction]-related code.
 */
class BlockchainTransactionException(override val message: String): Exception(message)