package com.commuto.interfacedesktop.blockchain

import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

/**
 * Provides gas prices to [com.commuto.interfacedesktop.CommutoSwap] instances. Note that
 * this provides a constant gas price and gas limit instead of calculating the most economical
 * values, so it is wasteful.
 */
class DumbGasProvider : ContractGasProvider {
    /**
     * Provides the transaction gas price to specify when calling a specific function.
     *
     * @param contractFunc The contract function that will be called in a transaction with a gas
     * price specified by this method.
     *
     * @return A gas price as a [BigInteger].
     */
    override fun getGasPrice(contractFunc: String?): BigInteger {
        return BigInteger.valueOf(875000000)
    }

    /**
     * Provides the gas price to specify in a transaction.
     *
     * @return A gas price as a [BigInteger].
     */
    @Deprecated("Deprecated in Java")
    override fun getGasPrice(): BigInteger {
        return BigInteger.valueOf(875000000)
    }

    /**
     * Provides the transaction gas limit to specify when calling a specific function.
     *
     * @param contractFunc The contract function that will be called in a transaction with a gas
     * limit specified by this method.
     *
     * @return A gas price as a [BigInteger].
     */
    override fun getGasLimit(contractFunc: String?): BigInteger {
        return BigInteger.valueOf(30000000)
    }

    /**
     * Provides a gas limit to specify in a transaction.
     *
     * @return A gas limit as a [BigInteger].
     */
    @Deprecated("Deprecated in Java")
    override fun getGasLimit(): BigInteger {
        return BigInteger.valueOf(30000000)
    }
}