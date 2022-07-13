package com.commuto.interfacedesktop.blockchain.structs

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger

/**
 * Represents an on-chain [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) struct.
 *
 * @param isCreated Corresponds to an on-chain Offer's `isCreated` property.
 * @param isTaken Corresponds to an on-chain Offer's `isTaken` property.
 * @param maker Corresponds to an on-chain Offer's `maker` property.
 * @param interfaceID Corresponds ot an on-chain Offer's `interfaceId` property.
 * @param stablecoin Corresponds to an on-chain Offer's `stablecoin` property.
 * @param amountLowerBound Corresponds to an on-chain Offer's `amountLowerBound` property.
 * @param amountUpperBound Corresponds to an on-chain Offer's `amountUpperBound` property.
 * @param securityDepositAmount Corresponds to an on-chain Offer's `securityDepositAmount` property.
 * @param serviceFeeRate Corresponds to an on-chain Offer's `serviceFeeRate` property.
 * @param direction Corresponds to an on-chain Offer's `direction` property.
 * @param settlementMethods Corresponds to an on-chain Offer's `settlementMethods` property.
 * @param protocolVersion Corresponds to an on-chain Offer's `protocolVersion` property.
 * @param chainID Corresponds to an on-chain Offer's `chainID` property.
 */
data class OfferStruct(
    val isCreated: Boolean,
    val isTaken: Boolean,
    val maker: String,
    val interfaceID: ByteArray,
    val stablecoin: String,
    val amountLowerBound: BigInteger,
    val amountUpperBound: BigInteger,
    val securityDepositAmount: BigInteger,
    val serviceFeeRate: BigInteger,
    val direction: BigInteger,
    val settlementMethods: List<ByteArray>,
    val protocolVersion: BigInteger,
    val chainID: BigInteger
) {
    /**
     * Creates an [OfferStruct] from a [CommutoSwap.Offer] and the specified [chainID].
     *
     * @param offer The [CommutoSwap.Offer] from which the returned [OfferStruct] will be created.
     * @param chainID The ID of the blockchain on which this offer exists.
     */
    private constructor(offer: CommutoSwap.Offer, chainID: BigInteger): this(
        offer.isCreated,
        offer.isTaken,
        offer.maker,
        offer.interfaceId,
        offer.stablecoin,
        offer.amountLowerBound,
        offer.amountUpperBound,
        offer.securityDepositAmount,
        offer.serviceFeeRate,
        offer.direction,
        offer.settlementMethods,
        offer.protocolVersion,
        chainID
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfferStruct

        if (isCreated != other.isCreated) return false
        if (isTaken != other.isTaken) return false
        if (maker != other.maker) return false
        if (!interfaceID.contentEquals(other.interfaceID)) return false
        if (stablecoin != other.stablecoin) return false
        if (amountLowerBound != other.amountLowerBound) return false
        if (amountUpperBound != other.amountUpperBound) return false
        if (securityDepositAmount != other.securityDepositAmount) return false
        if (serviceFeeRate != other.serviceFeeRate) return false
        if (direction != other.direction) return false
        if (settlementMethods != other.settlementMethods) return false
        if (protocolVersion != other.protocolVersion) return false
        if (chainID != other.chainID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCreated.hashCode()
        result = 31 * result + isTaken.hashCode()
        result = 31 * result + maker.hashCode()
        result = 31 * result + interfaceID.contentHashCode()
        result = 31 * result + stablecoin.hashCode()
        result = 31 * result + amountLowerBound.hashCode()
        result = 31 * result + amountUpperBound.hashCode()
        result = 31 * result + securityDepositAmount.hashCode()
        result = 31 * result + serviceFeeRate.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + settlementMethods.hashCode()
        result = 31 * result + protocolVersion.hashCode()
        result = 31 * result + chainID.hashCode()
        return result
    }

    companion object {
        /**
         * Creates an [OfferStruct] from a [CommutoSwap.Offer] and the specified [chainID] if
         * [CommutoSwap.Offer.isCreated] is true, or returns null if false.
         *
         * @param offer The [CommutoSwap.Offer] from which the returned [OfferStruct] will be created.
         * @param chainID The ID of the blockchain on which this offer exists.
         *
         * @return An [OfferStruct] derived from [offer] and [chainID] if [CommutoSwap.Offer.isCreated] is true, or null
         * if false.
         */
        fun createFromGetOfferResponse(offer: CommutoSwap.Offer, chainID: BigInteger): OfferStruct? {
            return if (!offer.isCreated) {
                null
            } else {
                OfferStruct(offer, chainID)
            }
        }
    }

}