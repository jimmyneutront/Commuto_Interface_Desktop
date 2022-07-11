package com.commuto.interfacedesktop.offer

import java.math.BigInteger
import java.util.UUID

/**
 * Represents an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param id The ID that uniquely identifies the offer, as a [UUID].
 * @param isCreated Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s isCreated property.
 * @param isTaken Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s isTaken property.
 * @param maker Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s maker property.
 * @param interfaceId Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s interfaceId property.
 * @param stablecoin Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s stablecoin property.
 * @param amountLowerBound Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s amountLowerBound property.
 * @param amountUpperBound Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s amountUpperBound property.
 * @param securityDepositAmount Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s securityDepositAmount property.
 * @param serviceFeeRate Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s serviceFeeRate property.
 * @param onChainDirection Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s direction property.
 * @param settlementMethods Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s settlementMethods property.
 * @param protocolVersion Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s protocolVersion property.
 * @param chainID The ID of the blockchain on which this Offer exists.
 *
 * @property direction The direction of the offer, indicating whether the maker is offering to buy stablecoin or sell
 * stablecoin.
 */
data class Offer(
    val isCreated: Boolean,
    val isTaken: Boolean,
    val id: UUID,
    val maker: String,
    val interfaceId: ByteArray,
    val stablecoin: String,
    val amountLowerBound: BigInteger,
    val amountUpperBound: BigInteger,
    val securityDepositAmount: BigInteger,
    val serviceFeeRate: BigInteger,
    val onChainDirection: BigInteger,
    var settlementMethods: List<ByteArray>,
    val protocolVersion: BigInteger,
    val chainID: BigInteger,
) {

    val direction: OfferDirection

    init {
        when (this.onChainDirection) {
            BigInteger.ZERO -> {
                this.direction = OfferDirection.BUY
            }
            BigInteger.ONE -> {
                this.direction = OfferDirection.SELL
            }
            else -> {
                throw IllegalStateException("Unexpected onChainDirection encountered while creating Offer")
            }
        }
    }


    companion object {
        /**
         * A [List] of sample [Offer]s. Used for previewing offer-related Composable functions.
         */
        val sampleOffers = listOf(
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F", // DAI on Ethereum Mainnet
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ZERO,
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
            ),
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", // USDC on Ethereum Mainnet
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(6),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(6),
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ONE,
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
            ),
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x4Fabb145d64652a948d72533023f6E7A623C7C53", // BUSD on Ethereum Mainnet
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ONE,
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
            ),
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                /*
                UniswapV3Factory on Ethereum Mainnet, definitely not a stablecoin contract
                 */
                stablecoin = "0x1F98431c8aD98523631AE4a59f267346ea31F984",
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ONE,
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
            ),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Offer

        if (isCreated != other.isCreated) return false
        if (isTaken != other.isTaken) return false
        if (id != other.id) return false
        if (maker != other.maker) return false
        if (!interfaceId.contentEquals(other.interfaceId)) return false
        if (stablecoin != other.stablecoin) return false
        if (amountLowerBound != other.amountLowerBound) return false
        if (amountUpperBound != other.amountUpperBound) return false
        if (securityDepositAmount != other.securityDepositAmount) return false
        if (serviceFeeRate != other.serviceFeeRate) return false
        if (onChainDirection != other.onChainDirection) return false
        if (settlementMethods != other.settlementMethods) return false
        if (protocolVersion != other.protocolVersion) return false
        if (chainID != other.chainID) return false
        if (direction != other.direction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCreated.hashCode()
        result = 31 * result + isTaken.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + maker.hashCode()
        result = 31 * result + interfaceId.contentHashCode()
        result = 31 * result + stablecoin.hashCode()
        result = 31 * result + amountLowerBound.hashCode()
        result = 31 * result + amountUpperBound.hashCode()
        result = 31 * result + securityDepositAmount.hashCode()
        result = 31 * result + serviceFeeRate.hashCode()
        result = 31 * result + onChainDirection.hashCode()
        result = 31 * result + settlementMethods.hashCode()
        result = 31 * result + protocolVersion.hashCode()
        result = 31 * result + chainID.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }

}