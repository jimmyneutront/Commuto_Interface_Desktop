package com.commuto.interfacedesktop.blockchain

import org.web3j.crypto.RawTransaction
import java.math.BigInteger
import java.util.Date

/**
 * A wrapper around a [RawTransaction] that was created by this interface and should be monitored by this interface for
 * confirmation, transaction dropping, transaction failure and transaction success. This also contains the date and time
 * at which this transaction was created, as well as the latest block number of the blockchain at the time when this
 * transaction was created.
 *
 * @property transaction The [RawTransaction] that this wraps.
 * @property timeOfCreation The date and time at which this was created.
 * @property latestBlockNumberAtCreation The number of the latest block in the blockchain at the time when this
 * transaction was created.
 * @property type Describes what [transaction] does. (For example: one that opens a new offer, one that fills a swap,
 * etc).
 */
data class BlockchainTransaction(
    val transaction: RawTransaction,
    val timeOfCreation: Date = Date(),
    val latestBlockNumberAtCreation: BigInteger,
    val type: BlockchainTransactionType,
) {
}