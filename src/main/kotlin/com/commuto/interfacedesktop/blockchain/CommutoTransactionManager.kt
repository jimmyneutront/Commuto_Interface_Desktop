package com.commuto.interfacedesktop.blockchain

import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction

import org.web3j.protocol.Web3j

import org.web3j.tx.RawTransactionManager
import java.io.IOException
import java.math.BigInteger


class CommutoTransactionManager(web3j: Web3j?, credentials: Credentials?, chainId: Long) :
    RawTransactionManager(web3j, credentials, chainId) {
    @Throws(IOException::class)
    fun getTransactionHash(
        gasPrice: BigInteger?,
        gasLimit: BigInteger?,
        to: String?,
        data: String?,
        value: BigInteger?,
        constructor: Boolean
    ): String {
        val nonce: BigInteger = this.nonce
        val rawTransaction =
            RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
        val hexValue = sign(rawTransaction)
        return Hash.sha3(hexValue)
    }
}