package com.commuto.interfacedesktop.offer

import java.math.BigInteger
import java.util.UUID

/**
 * Represents an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param id The ID that uniquely identifies the offer, as a [UUID].
 * @param direction The direction of the offer, indicating whether the maker is offering to buy
 * stablecoin or sell stablecoin.
 * @param price The price at which the maker is offering to buy/sell stablecoin, as the cost in
 * FIAT per one STBL.
 * @param pair A string of the form "FIAT/STBL", where FIAT is the abbreviation of the fiat currency
 * being exchanged, and STBL is the ticker symbol of the stablecoin being exchanged.
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
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s onChainDirection property.
 * @param onChainPrice Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s onChainPrice property.
 * @param settlementMethods Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s settlementMethods property.
 * @param protocolVersion Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s protocolVersion property.
 */
data class Offer(
    val id: UUID,
    var direction: String,
    var price: String,
    var pair: String,
    val isCreated: Boolean,
    val isTaken: Boolean,
    val maker: String,
    val interfaceId: ByteArray,
    val stablecoin: String,
    val amountLowerBound: BigInteger,
    val amountUpperBound: BigInteger,
    val securityDepositAmount: BigInteger,
    val serviceFeeRate: BigInteger,
    val onChainDirection: BigInteger,
    val onChainPrice: ByteArray,
    val settlementMethods: List<ByteArray>,
    val protocolVersion: BigInteger
) {
    companion object {
        /**
         * A [List] of sample [Offer]s. Used for previewing offer-related Composable functions.
         */
        val sampleOffers = listOf(
            Offer(
                id = UUID.randomUUID(),
                direction = "Buy",
                price = "1.004",
                pair = "USD/USDT",
                isCreated = true,
                isTaken = false,
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x0000000000000000000000000000000000000000",
                amountLowerBound = BigInteger.ZERO,
                amountUpperBound = BigInteger.ZERO,
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ZERO,
                onChainPrice = ByteArray(0),
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO
            ),
            Offer(
                id = UUID.randomUUID(),
                direction = "Sell",
                price = "1.002",
                pair = "GBP/USDC",
                isCreated = true,
                isTaken = false,
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x0000000000000000000000000000000000000000",
                amountLowerBound = BigInteger.ZERO,
                amountUpperBound = BigInteger.ZERO,
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ZERO,
                onChainPrice = ByteArray(0),
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO
            ),
            Offer(
                id = UUID.randomUUID(),
                direction = "Sell",
                price = "0.997",
                pair = "PLN/LUSD",
                isCreated = true,
                isTaken = false,
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x0000000000000000000000000000000000000000",
                amountLowerBound = BigInteger.ZERO,
                amountUpperBound = BigInteger.ZERO,
                securityDepositAmount = BigInteger.ZERO,
                serviceFeeRate = BigInteger.ZERO,
                onChainDirection = BigInteger.ZERO,
                onChainPrice = ByteArray(0),
                settlementMethods = listOf(ByteArray(0)),
                protocolVersion = BigInteger.ZERO
            ),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Offer

        if (id != other.id) return false
        if (direction != other.direction) return false
        if (price != other.price) return false
        if (pair != other.pair) return false
        if (isCreated != other.isCreated) return false
        if (isTaken != other.isTaken) return false
        if (maker != other.maker) return false
        if (!interfaceId.contentEquals(other.interfaceId)) return false
        if (stablecoin != other.stablecoin) return false
        if (amountLowerBound != other.amountLowerBound) return false
        if (amountUpperBound != other.amountUpperBound) return false
        if (securityDepositAmount != other.securityDepositAmount) return false
        if (serviceFeeRate != other.serviceFeeRate) return false
        if (onChainDirection != other.onChainDirection) return false
        if (!onChainPrice.contentEquals(other.onChainPrice)) return false
        if (settlementMethods != other.settlementMethods) return false
        if (protocolVersion != other.protocolVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + pair.hashCode()
        result = 31 * result + isCreated.hashCode()
        result = 31 * result + isTaken.hashCode()
        result = 31 * result + maker.hashCode()
        result = 31 * result + interfaceId.contentHashCode()
        result = 31 * result + stablecoin.hashCode()
        result = 31 * result + amountLowerBound.hashCode()
        result = 31 * result + amountUpperBound.hashCode()
        result = 31 * result + securityDepositAmount.hashCode()
        result = 31 * result + serviceFeeRate.hashCode()
        result = 31 * result + onChainDirection.hashCode()
        result = 31 * result + onChainPrice.contentHashCode()
        result = 31 * result + settlementMethods.hashCode()
        result = 31 * result + protocolVersion.hashCode()
        return result
    }
}