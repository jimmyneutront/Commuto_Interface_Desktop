package com.commuto.interfacedesktop.ui

import java.math.BigInteger

/**
 * A structure that provides currency code and name information about a stablecoin, given its contract address and chain
 * ID.
 *
 * @param stablecoinInformation Nested dictionaries containing stablecoin information. The inner dictionary maps `
 * Ethereum addresses of stablecoin smart contracts to [StablecoinInformation]s containing information about the
 * stablecoin corresponding to their Ethereum address key. The outer dictionary maps blockchain IDs to inner maps,
 * containing information about all the supported stablecoin contracts on the blockchain with the specified ID.
 */
class StablecoinInformationRepository(
    val stablecoinInformation: Map<BigInteger, Map<String, StablecoinInformation>>
) {

    /**
     * Returns a [StablecoinInformation] for the stablecoin at the specified [contractAddress] on the blockchain with
     * the specified [chainID], or null if no such [StablecoinInformation] is found.
     *
     * @param chainID The ID of the blockchain on which the stablecoin for which to get information exists.
     * @param contractAddress The contract address of the stablecoin for which to get information.
     *
     * @return A [StablecoinInformation] for the stablecoin with the specified [chainID] and [contractAddress], or
     * null if no such [StablecoinInformation] is found.
     */
    fun getStablecoinInformation(chainID: BigInteger, contractAddress: String?): StablecoinInformation? {
        return stablecoinInformation[chainID]?.get(contractAddress)
    }

    companion object {
        /**
         * A sample [StablecoinInformationRepository] for testing purposes, which contains [StablecoinInformation]
         * for several stablecoin contracts on the Ethereum Mainnet.
         */
        val ethereumMainnetStablecoinInfoRepo = StablecoinInformationRepository(
            mapOf(
                BigInteger.ONE to mapOf(
                    "0x6B175474E89094C44Da98b954EedeAC495271d0F" to StablecoinInformation(
                        "DAI",
                        "Dai",
                        18,
                    ),
                    "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48" to StablecoinInformation(
                        "USDC",
                        "USD Coin",
                        6,
                    ),
                    "0x4Fabb145d64652a948d72533023f6E7A623C7C53" to StablecoinInformation(
                        "BUSD",
                        "Binance USD",
                        18,
                    ),
                    "0xdAC17F958D2ee523a2206206994597C13D831ec7" to StablecoinInformation(
                        "USDT",
                        "USD Tether",
                        6,
                    ),
                )
            )
        )

        /**
         * A simple [StablecoinInformationRepository] for testing purposes, which contains [StablecoinInformation] for
         * several stablecoin contracts on hardhat test networks set up by
         * [CommuttoSwapTest.py](https://github.com/jimmyneutront/commuto-protocol/blob/main/tests/CommutoSwapTest.py).
         */
        val hardhatStablecoinInfoRepo = StablecoinInformationRepository(
            mapOf(
                BigInteger.ONE to mapOf(
                    "0x663F3ad617193148711d28f5334eE4Ed07016602" to StablecoinInformation(
                        "DAI",
                        "Dai",
                        18,
                    ),
                    "0x2E983A1Ba5e8b38AAAeC4B440B9dDcFBf72E15d1" to StablecoinInformation(
                        "USDC",
                        "USD Coin",
                        6,
                    ),
                    "0x8438Ad1C834623CfF278AB6829a248E37C2D7E3f" to StablecoinInformation(
                        "BUSD",
                        "Binance USD",
                        18,
                    ),
                    "0xBC9129Dc0487fc2E169941C75aABC539f208fb01" to StablecoinInformation(
                        "USDT",
                        "USD Tether",
                        6,
                    ),
                )
            )
        )
    }

}

/**
 * An object containing information about a specific stablecoin.
 *
 * @param currencyCode The stablecoin's currency code, such as "DAI" or "LUSD".
 * @param name The stablecoin's name, such as "Liquity USD" or "USD Coin".
 * @param decimal The number of token base units that make up one token unit. For example, DAI has a decimal value of
 * 18, so one US Dollar = 10^18 DAI token base units. So when we display a DAI value to the user, we display the number
 * of DAI token base units divided by 10^18.
 */
data class StablecoinInformation(val currencyCode: String, val name: String, val decimal: Int)