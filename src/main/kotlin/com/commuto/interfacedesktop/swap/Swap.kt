package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.structs.SwapStruct
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.SettlementMethod
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.util.*

/**
 * Represents a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
 *
 * @param isCreated Corresponds to an on-chain Swap's `isCreated` property.
 * @param requiresFill Corresponds to an on-chain Swap's `requiresFill` property.
 * @param id The ID that uniquely identifies this swap, as a [UUID].
 * @param maker Corresponds to an on-chain Swap's `maker` property.
 * @param makerInterfaceID Corresponds to an on-chain Swap's `makerInterfaceId` property.
 * @param taker Corresponds to an on-chain Swap's `taker` property.
 * @param takerInterfaceID Corresponds to an on-chain Swap's `takerInterfaceId` property.
 * @param stablecoin Corresponds to an on-chain Swap's `stablecoin` property.
 * @param amountLowerBound Corresponds to an on-chain Swap's `amountLowerBound` property.
 * @param amountUpperBound Corresponds to an on-chain Swap's `amountUpperBound` property.
 * @param securityDepositAmount Corresponds to an on-chain Swap's `securityDepositAmount` property.
 * @param takenSwapAmount Corresponds to an on-chain Swap's `takenSwapAmount` property.
 * @param serviceFeeAmount Corresponds to an on-chain Swap's `serviceFeeAmount` property.
 * @param serviceFeeRate Corresponds to an on-chain Swap's `serviceFeeRate` property.
 * @param direction The direction of the swap, indicating whether the maker is buying stablecoin or selling stablecoin.
 * @param onChainSettlementMethod Corresponds to an on-chain Swap's `settlementMethod` property.
 * @param protocolVersion Corresponds to an on-chain Swap's `protocolVersion` property.
 * @param isPaymentSent Corresponds to an on-chain Swap's `isPaymentSent` property.
 * @param isPaymentReceived Corresponds to an on-chain Swap's `isPaymentReceived` property.
 * @param hasBuyerClosed Corresponds to an on-chain Swap's `hasBuyerClosed` property.
 * @param onChainDisputeRaiser Corresponds to an on-chain Swap's `disputeRaiser` property.
 * @param chainID The ID of the blockchain on which this Swap exists.
 * @param state Indicates the current state of this swap, as described in the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * @param role Indicates the interface user's role for this swap.
 *
 * @property onChainDirection Corresponds to an on-chain Swap's `direction` property.
 * @property settlementMethod A [SettlementMethod] derived by deserializing [onChainSettlementMethod].
 */
data class Swap(
    val isCreated: Boolean,
    val requiresFill: Boolean,
    val id: UUID,
    val maker: String,
    val makerInterfaceID: ByteArray,
    val taker: String,
    val takerInterfaceID: ByteArray,
    val stablecoin: String,
    val amountLowerBound: BigInteger,
    val amountUpperBound: BigInteger,
    val securityDepositAmount: BigInteger,
    val takenSwapAmount: BigInteger,
    val serviceFeeAmount: BigInteger,
    val serviceFeeRate: BigInteger,
    val direction: OfferDirection,
    val onChainSettlementMethod: ByteArray,
    val protocolVersion: BigInteger,
    val isPaymentSent: Boolean,
    val isPaymentReceived: Boolean,
    val hasBuyerClosed: Boolean,
    val hasSellerClosed: Boolean,
    val onChainDisputeRaiser: BigInteger,
    val chainID: BigInteger,
    var state: SwapState,
    val role: SwapRole,
) {

    val onChainDirection: BigInteger
    val settlementMethod: SettlementMethod

    init {
        when (this.direction) {
            OfferDirection.BUY -> {
                this.onChainDirection = BigInteger.ZERO
            }
            OfferDirection.SELL -> {
                this.onChainDirection = BigInteger.ONE
            }
        }
        settlementMethod = Json.decodeFromString(onChainSettlementMethod.decodeToString())
    }

    /**
     * Returns an [SwapStruct] derived from this [Swap].
     *
     * @return A [SwapStruct] derived from this [Swap].
     */
    fun toSwapStruct(): SwapStruct {
        return SwapStruct(
            isCreated = isCreated,
            requiresFill = requiresFill,
            maker = maker,
            makerInterfaceID = makerInterfaceID,
            taker = taker,
            takerInterfaceID = takerInterfaceID,
            stablecoin = stablecoin,
            amountLowerBound = amountLowerBound,
            amountUpperBound = amountUpperBound,
            securityDepositAmount = securityDepositAmount,
            takenSwapAmount = takenSwapAmount,
            serviceFeeAmount = serviceFeeAmount,
            serviceFeeRate = serviceFeeRate,
            direction = onChainDirection,
            settlementMethod = onChainSettlementMethod,
            protocolVersion = protocolVersion,
            isPaymentSent = isPaymentSent,
            isPaymentReceived = isPaymentReceived,
            hasBuyerClosed = hasBuyerClosed,
            hasSellerClosed = hasSellerClosed,
            disputeRaiser = onChainDisputeRaiser,
            chainID = chainID
        )
    }
    companion object {
        /**
         * A [List] of sample [Swap]s. Used for previewing swap-related Composable functions.
         */
        val sampleSwaps = listOf(
            Swap(
                isCreated = true,
                requiresFill = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                makerInterfaceID = ByteArray(0),
                taker = "0x0000000000000000000000000000000000000000",
                takerInterfaceID = ByteArray(0),
                stablecoin = "0x663F3ad617193148711d28f5334eE4Ed07016602", // DAI on Hardhat,
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.valueOf(2_000) * BigInteger.TEN.pow(18),
                takenSwapAmount = BigInteger.valueOf(15_000) * BigInteger.TEN.pow(18),
                serviceFeeAmount = BigInteger.valueOf(150) * BigInteger.TEN.pow(18),
                serviceFeeRate = BigInteger.valueOf(100),
                direction = OfferDirection.BUY,
                onChainSettlementMethod =
                    """
                    {
                        "f": "EUR",
                        "p": "0.94",
                        "m": "SEPA"
                    }
                    """.trimIndent().encodeToByteArray(),
                protocolVersion = BigInteger.ZERO,
                isPaymentSent = false,
                isPaymentReceived = false,
                hasBuyerClosed = false,
                hasSellerClosed = false,
                onChainDisputeRaiser = BigInteger.ZERO,
                chainID = BigInteger.valueOf(31337L), // Hardhat blockchain ID,
                state = SwapState.TAKE_OFFER_TRANSACTION_BROADCAST,
                role = SwapRole.MAKER_AND_BUYER,
            ),
            Swap(
                isCreated = true,
                requiresFill = false,
                id = UUID.randomUUID(),
                maker = "0x0000000000000000000000000000000000000000",
                makerInterfaceID = ByteArray(0),
                taker = "0x0000000000000000000000000000000000000000",
                takerInterfaceID = ByteArray(0),
                stablecoin = "0x2E983A1Ba5e8b38AAAeC4B440B9dDcFBf72E15d1", // USDC on Hardhat,
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(18),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(18),
                securityDepositAmount = BigInteger.valueOf(2_000) * BigInteger.TEN.pow(18),
                takenSwapAmount = BigInteger.valueOf(15_000) * BigInteger.TEN.pow(18),
                serviceFeeAmount = BigInteger.valueOf(150) * BigInteger.TEN.pow(18),
                serviceFeeRate = BigInteger.valueOf(100),
                direction = OfferDirection.BUY,
                onChainSettlementMethod =
                    """
                    {
                        "f": "USD",
                        "p": "1.00",
                        "m": "SWIFT"
                    }
                    """.trimIndent().encodeToByteArray(),
                protocolVersion = BigInteger.ZERO,
                isPaymentSent = false,
                isPaymentReceived = false,
                hasBuyerClosed = false,
                hasSellerClosed = false,
                onChainDisputeRaiser = BigInteger.ZERO,
                chainID = BigInteger.valueOf(31337L), // Hardhat blockchain ID,
                state = SwapState.TAKE_OFFER_TRANSACTION_BROADCAST,
                role = SwapRole.TAKER_AND_SELLER,
            ),
        )
    }
}