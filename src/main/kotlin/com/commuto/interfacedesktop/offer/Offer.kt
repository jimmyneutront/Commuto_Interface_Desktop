package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.commuto.interfacedesktop.blockchain.structs.OfferStruct
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
 * @property direction The direction of the offer, indicating whether the maker is offering to buy stablecoin or sell
 * stablecoin.
 * @property settlementMethods A [SnapshotStateList] of [SettlementMethod]s derived from parsing
 * [onChainSettlementMethods].
 * @param protocolVersion Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s protocolVersion property.
 * @param chainID The ID of the blockchain on which this Offer exists.
 * @param havePublicKey Indicates whether this interface has a copy of the public key specified by the [interfaceId]
 * property.
 * @param isUserMaker Indicates whether the user of this interface is the maker of this offer.
 *
 * @property serviceFeeAmountLowerBound The minimum service fee for the new offer.
 * @property serviceFeeAmountUpperBound The maximum service fee for the new offer.
 * @property onChainDirection Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s direction property.
 * @property onChainSettlementMethods Corresponds to an on-chain
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)'s settlementMethods property.
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
    val direction: OfferDirection,
    var settlementMethods: SnapshotStateList<SettlementMethod>,
    val protocolVersion: BigInteger,
    val chainID: BigInteger,
    var havePublicKey: Boolean,
    var isUserMaker: Boolean,
) {

    constructor(
        isCreated: Boolean,
        isTaken: Boolean,
        id: UUID,
        maker: String,
        interfaceId: ByteArray,
        stablecoin: String,
        amountLowerBound: BigInteger,
        amountUpperBound: BigInteger,
        securityDepositAmount: BigInteger,
        serviceFeeRate: BigInteger,
        onChainDirection: BigInteger,
        onChainSettlementMethods: List<ByteArray>,
        protocolVersion: BigInteger,
        chainID: BigInteger,
        havePublicKey: Boolean,
        isUserMaker: Boolean
    ) : this(
        isCreated = isCreated,
        isTaken = isTaken,
        id = id,
        maker = maker,
        interfaceId = interfaceId,
        stablecoin = stablecoin,
        amountLowerBound = amountLowerBound,
        amountUpperBound = amountUpperBound,
        securityDepositAmount = securityDepositAmount,
        serviceFeeRate = serviceFeeRate,
        direction =
        when (onChainDirection) {
            BigInteger.ZERO -> {
                OfferDirection.BUY
            }
            BigInteger.ONE -> {
                OfferDirection.SELL
            }
            else -> {
                throw IllegalStateException("Unexpected onChainDirection encountered while creating Offer")
            }
        },
        settlementMethods = kotlin.run {
            val settlementMethods = mutableStateListOf<SettlementMethod>()
            onChainSettlementMethods.forEach {
                try {
                    settlementMethods.add(Json.decodeFromString(it.decodeToString()))
                } catch (_: Exception) {
                }
            }
            settlementMethods
        },
        protocolVersion = protocolVersion,
        chainID = chainID,
        havePublicKey = havePublicKey,
        isUserMaker = isUserMaker
    )

    val serviceFeeAmountLowerBound: BigInteger = this.serviceFeeRate * (this.amountLowerBound /
            BigInteger.valueOf(10_000L))
    val serviceFeeAmountUpperBound: BigInteger = this.serviceFeeRate * (this.amountUpperBound /
            BigInteger.valueOf(10_000L))
    val onChainDirection: BigInteger
    var onChainSettlementMethods: List<ByteArray>

    init {
        when (this.direction) {
            OfferDirection.BUY -> {
                this.onChainDirection = BigInteger.ZERO
            }
            OfferDirection.SELL -> {
                this.onChainDirection = BigInteger.ONE
            }
        }
        val onChainSettlementMethods = mutableListOf<ByteArray>()
        settlementMethods.forEach {
            onChainSettlementMethods.add(Json.encodeToString(it).encodeToByteArray())
        }
        this.onChainSettlementMethods = onChainSettlementMethods
    }

    /**
     * Returns an [OfferStruct] derived from this [Offer].
     *
     * @return An [OfferStruct] derived from this [Offer].
     */
    fun toOfferStruct(): OfferStruct {
        return OfferStruct(
            isCreated = this.isCreated,
            isTaken = this.isTaken,
            maker = this.maker,
            interfaceID = this.interfaceId,
            stablecoin = this.stablecoin,
            amountLowerBound = this.amountLowerBound,
            amountUpperBound = this.amountUpperBound,
            securityDepositAmount = this.securityDepositAmount,
            serviceFeeRate = this.serviceFeeRate,
            direction = this.onChainDirection,
            settlementMethods = this.onChainSettlementMethods,
            protocolVersion = this.protocolVersion,
            chainID = this.chainID
        )
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
                stablecoin = "0x663F3ad617193148711d28f5334eE4Ed07016602", // DAI on Hardhat
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.valueOf(1_000) * BigInteger.TEN.pow(18),
                serviceFeeRate = BigInteger.valueOf(100),
                onChainDirection = BigInteger.ZERO,
                onChainSettlementMethods = listOf(
                    """
                    {
                        "f": "EUR",
                        "p": "0.94",
                        "m": "SEPA"
                    }
                    """.trimIndent().encodeToByteArray()
                ),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
                havePublicKey = false,
                isUserMaker = false,
            ),
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x2E983A1Ba5e8b38AAAeC4B440B9dDcFBf72E15d1", // USDC on Hardhat
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(6),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(6),
                securityDepositAmount = BigInteger.valueOf(1_000) * BigInteger.TEN.pow(6),
                serviceFeeRate = BigInteger.valueOf(10),
                onChainDirection = BigInteger.ONE,
                onChainSettlementMethods = listOf(
                    """
                    {
                        "f": "USD",
                        "p": "1.00",
                        "m": "SWIFT"
                    }
                    """.trimIndent().encodeToByteArray()
                ),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
                havePublicKey = false,
                isUserMaker = false,
            ),
            Offer(
                isCreated = true,
                isTaken = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                interfaceId = ByteArray(0),
                stablecoin = "0x8438Ad1C834623CfF278AB6829a248E37C2D7E3f", // BUSD on Hardhat
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.valueOf(1_000) * BigInteger.TEN.pow(18),
                serviceFeeRate = BigInteger.valueOf(1),
                onChainDirection = BigInteger.ONE,
                onChainSettlementMethods = listOf("not valid JSON".encodeToByteArray()),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
                havePublicKey = false,
                isUserMaker = false,
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
                securityDepositAmount = BigInteger.valueOf(1_000) * BigInteger.TEN.pow(18),
                serviceFeeRate = BigInteger.valueOf(100),
                onChainDirection = BigInteger.ONE,
                onChainSettlementMethods = listOf("not valid JSON".encodeToByteArray()),
                protocolVersion = BigInteger.ZERO,
                chainID = BigInteger.ONE, // Ethereum Mainnet blockchain ID
                havePublicKey = false,
                isUserMaker = false,
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
        if (direction != other.direction) return false
        if (settlementMethods != other.settlementMethods) return false
        if (protocolVersion != other.protocolVersion) return false
        if (chainID != other.chainID) return false
        if (havePublicKey != other.havePublicKey) return false
        if (isUserMaker != other.isUserMaker) return false
        if (serviceFeeAmountLowerBound != other.serviceFeeAmountLowerBound) return false
        if (serviceFeeAmountUpperBound != other.serviceFeeAmountUpperBound) return false
        if (onChainDirection != other.onChainDirection) return false
        if (onChainSettlementMethods != other.onChainSettlementMethods) return false

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
        result = 31 * result + direction.hashCode()
        result = 31 * result + settlementMethods.hashCode()
        result = 31 * result + protocolVersion.hashCode()
        result = 31 * result + chainID.hashCode()
        result = 31 * result + havePublicKey.hashCode()
        result = 31 * result + isUserMaker.hashCode()
        result = 31 * result + serviceFeeAmountLowerBound.hashCode()
        result = 31 * result + serviceFeeAmountUpperBound.hashCode()
        result = 31 * result + onChainDirection.hashCode()
        result = 31 * result + onChainSettlementMethods.hashCode()
        return result
    }

}