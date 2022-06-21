package com.commuto.interfacedesktop.blockchain

import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction

import org.web3j.protocol.Web3j

import org.web3j.tx.RawTransactionManager
import java.io.IOException
import java.math.BigInteger

/**
 * The [RawTransactionManager] used by the [com.commuto.interfacemobile.android.CommutoSwap] instance
 * in [BlockchainService].
 *
 * @property web3j: This transaction manager's [Web3j] instance.
 * @property chainId: The ID of the blockchain to which [web3j] is connected.
 */
class CommutoTransactionManager(web3j: Web3j?, credentials: Credentials?, chainId: Long) :
    RawTransactionManager(web3j, credentials, chainId) {
    /**
     * Gets the hash of a transaction to the address specified as [to], with the specified
     * [gasPrice], [gasLimit], [data] and [value] with the next nonce of the account specified by
     * the [Credentials] object passed at [CommutoTransactionManager] creation.
     *
     * @param gasPrice The gas price of the transaction to be hashed.
     * @param gasLimit The gas limit of the transaction to be hashed.
     * @param to The destination address of the transaction to be hashed.
     * @param data The data of the transaction to be hashed.
     * @param value The amount to be transferred to [to] by the transaction to be hashed.
     *
     * @return The transaction hash, as a [String].
     */
    @Throws(IOException::class)
    fun getTransactionHash(
        gasPrice: BigInteger?,
        gasLimit: BigInteger?,
        to: String?,
        data: String?,
        value: BigInteger?
    ): String {
        val nonce: BigInteger = this.nonce
        val rawTransaction =
            RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
        val hexValue = sign(rawTransaction)
        return Hash.sha3(hexValue)
    }
}