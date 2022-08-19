package com.commuto.interfacedesktop.blockchain.structs

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger

/**
 * Represents an on-chain [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) struct.
 *
 * @param isCreated Corresponds to an on-chain Swap's `isCreated` property.
 * @param requiresFill Corresponds to an on-chain Swap's `requiresFill` property.
 * @param maker Corresponds to an on-chain Swap's `maker` property.
 * @param makerInterfaceID Corresponds to an on-chain Swap's `makerInterfaceID` property.
 * @param taker Corresponds to an on-chain Swap's `taker` property.
 * @param takerInterfaceID Corresponds to an on-chain Swap's `takerInterfaceID` property.
 * @param stablecoin Corresponds to an on-chain Swap's `stablecoin` property.
 * @param amountLowerBound Corresponds to an on-chain Swap's `amountLowerBound` property.
 * @param amountUpperBound Corresponds to an on-chain Swap's `amountUpperBound` property.
 * @param securityDepositAmount Corresponds to an on-chain Swap's `securityDepositAmount` property.
 * @param takenSwapAmount Corresponds to an on-chain Swap's `takenSwapAmount` property.
 * @param serviceFeeAmount Corresponds to an on-chain Swap's `serviceFeeAmount` property.
 * @param serviceFeeRate Corresponds to an on-chain Swap's `serviceFeeRate` property.
 * @param direction Corresponds to an on-chain Swap's `direction` property.
 * @param settlementMethod Corresponds to an on-chain Swap's `settlementMethod` property.
 * @param protocolVersion Corresponds to an on-chain Swap's `protocolVersion` property.
 * @param isPaymentSent Corresponds to an on-chain Swap's `isPaymentSent` property.
 * @param isPaymentReceived Corresponds to an on-chain Swap's `isPaymentReceived` property.
 * @param hasBuyerClosed Corresponds to an on-chain Swap's `hasBuyerClosed` property.
 * @param hasSellerClosed Corresponds to an on-chain Swap's `hasSellerClosed` property.
 * @param disputeRaiser Corresponds to an on-chain Swap's `disputeRaiser` property.
 * @param chainID Corresponds to an on-chain Swap's `chainID` property.
 */
data class SwapStruct(
    val isCreated: Boolean,
    val requiresFill: Boolean,
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
    val direction: BigInteger,
    val settlementMethod: ByteArray,
    val protocolVersion: BigInteger,
    val isPaymentSent: Boolean,
    val isPaymentReceived: Boolean,
    val hasBuyerClosed: Boolean,
    val hasSellerClosed: Boolean,
    val disputeRaiser: BigInteger,
    val chainID: BigInteger,
) {

    /**
     * Returns a [CommutoSwap.Swap] derived from this [SwapStruct].
     *
     * @return A [CommutoSwap.Swap] derived from this [SwapStruct]
     */
    fun toCommutoSwapSwap(): CommutoSwap.Swap {
        return CommutoSwap.Swap(
            this.isCreated,
            this.requiresFill,
            this.maker,
            this.makerInterfaceID,
            this.taker,
            this.takerInterfaceID,
            this.stablecoin,
            this.amountLowerBound,
            this.amountUpperBound,
            this.securityDepositAmount,
            this.takenSwapAmount,
            this.serviceFeeAmount,
            this.serviceFeeRate,
            this.direction,
            this.settlementMethod,
            this.protocolVersion,
            this.isPaymentSent,
            this.isPaymentReceived,
            this.hasBuyerClosed,
            this.hasSellerClosed,
            this.disputeRaiser,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SwapStruct

        if (isCreated != other.isCreated) return false
        if (requiresFill != other.requiresFill) return false
        if (maker != other.maker) return false
        if (!makerInterfaceID.contentEquals(other.makerInterfaceID)) return false
        if (taker != other.taker) return false
        if (!takerInterfaceID.contentEquals(other.takerInterfaceID)) return false
        if (stablecoin != other.stablecoin) return false
        if (amountLowerBound != other.amountLowerBound) return false
        if (amountUpperBound != other.amountUpperBound) return false
        if (securityDepositAmount != other.securityDepositAmount) return false
        if (takenSwapAmount != other.takenSwapAmount) return false
        if (serviceFeeAmount != other.serviceFeeAmount) return false
        if (serviceFeeRate != other.serviceFeeRate) return false
        if (direction != other.direction) return false
        if (!settlementMethod.contentEquals(other.settlementMethod)) return false
        if (protocolVersion != other.protocolVersion) return false
        if (isPaymentSent != other.isPaymentSent) return false
        if (isPaymentReceived != other.isPaymentReceived) return false
        if (hasBuyerClosed != other.hasBuyerClosed) return false
        if (hasSellerClosed != other.hasSellerClosed) return false
        if (disputeRaiser != other.disputeRaiser) return false
        if (chainID != other.chainID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCreated.hashCode()
        result = 31 * result + requiresFill.hashCode()
        result = 31 * result + maker.hashCode()
        result = 31 * result + makerInterfaceID.contentHashCode()
        result = 31 * result + taker.hashCode()
        result = 31 * result + takerInterfaceID.contentHashCode()
        result = 31 * result + stablecoin.hashCode()
        result = 31 * result + amountLowerBound.hashCode()
        result = 31 * result + amountUpperBound.hashCode()
        result = 31 * result + securityDepositAmount.hashCode()
        result = 31 * result + takenSwapAmount.hashCode()
        result = 31 * result + serviceFeeAmount.hashCode()
        result = 31 * result + serviceFeeRate.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + settlementMethod.contentHashCode()
        result = 31 * result + protocolVersion.hashCode()
        result = 31 * result + isPaymentSent.hashCode()
        result = 31 * result + isPaymentReceived.hashCode()
        result = 31 * result + hasBuyerClosed.hashCode()
        result = 31 * result + hasSellerClosed.hashCode()
        result = 31 * result + disputeRaiser.hashCode()
        result = 31 * result + chainID.hashCode()
        return result
    }

}