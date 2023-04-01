package com.commuto.interfacedesktop.dispute

import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.key.keys.SymmetricKey
import com.commuto.interfacedesktop.db.SwapAndDispute as DatabaseSwapAndDispute
import com.commuto.interfacedesktop.offer.OfferDirection
import java.math.BigInteger
import java.util.*

/**
 * Represents both a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) and a
 * [Dispute](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#dispute), and should only be used by dispute
 * agents to represent swaps and disputes for which they have been selected as a dispute agent.
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
 * @param disputeRaisedBlockNumber Corresponds to an on-chain Dispute's `disputeRaisedBlockNumber` property.
 * @param disputeAgent0 Corresponds to an on-chain Dispute's `disputeAgent0` property.
 * @param disputeAgent1 Corresponds to an on-chain Dispute's `disputeAgent1` property.
 * @param disputeAgent2 Corresponds to an on-chain Dispute's `disputeAgent2` property.
 * @param hasDisputeAgent0Proposed Corresponds to an on-chain Dispute's `hasDisputeAgent0Proposed` property.
 * @param disputeAgent0MakerPayout Corresponds to an on-chain Dispute's `disputeAgent0MakerPayout` property.
 * @param disputeAgent0TakerPayout Corresponds to an on-chain Dispute's `disputeAgent0TakerPayout` property.
 * @param disputeAgent0ConfiscationPayout Corresponds to an on-chain Dispute's `disputeAgent0ConfiscationPayout`
 * property.
 * @param hasDisputeAgent1Proposed Corresponds to an on-chain Dispute's `hasDisputeAgent1Proposed` property.
 * @param disputeAgent1MakerPayout Corresponds to an on-chain Dispute's `disputeAgent1MakerPayout` property.
 * @param disputeAgent1TakerPayout Corresponds to an on-chain Dispute's `disputeAgent1TakerPayout` property.
 * @param disputeAgent1ConfiscationPayout Corresponds to an on-chain Dispute's `disputeAgent1ConfiscationPayout`
 * property.
 * @param hasDisputeAgent2Proposed Corresponds to an on-chain Dispute's `hasDisputeAgent2Proposed` property.
 * @param disputeAgent2MakerPayout Corresponds to an on-chain Dispute's `disputeAgent2MakerPayout` property.
 * @param disputeAgent2TakerPayout Corresponds to an on-chain Dispute's `disputeAgent2TakerPayout` property.
 * @param disputeAgent2ConfiscationPayout Corresponds to an on-chain Dispute's `disputeAgent2ConfiscationPayout`
 * property.
 * @param onChainMatchingProposals Corresponds to an on-chain Dispute's `onChainMatchingProposals` property.
 * @param makerReaction Corresponds to an on-chain Dispute's `makerReaction` property.
 * @param takerReaction Corresponds to an on-chain Dispute's `takerReaction` property.
 * @param onChainState Corresponds to an on-chain Dispute's `state` property.
 * @param hasMakerPaidOut Corresponds to an on-chain Dispute's `hasMakerPaidOut` property.
 * @param hasTakerPaidOut Corresponds to an on-chain Dispute's `hasTakerPaidOut` property.
 * @param totalWithoutSpentServiceFees Corresponds to an on-chain Dispute's `totalWithoutSpentServiceFees` property.
 * @param role The role of the user in this dispute.
 * @property disputeAgent0InterfaceID The interface ID derived from the first dispute agent's public key, or null if
 * this key has not yet been created/obtained.
 * @property state Indicates the current state of this SwapAndDispute from the perspective of a dispute agent, as
 * described in the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * @property makerCommunicationKey The [SymmetricKey] with which dispute agents communicate with the maker of the
 * disputed swap.
 * @property takerCommunicationKey The [SymmetricKey] with which dispute agents communicate with the taker of the
 * disputed swap.
 * @property disputeAgentCommunicationKey The [SymmetricKey] with which dispute agents communicate with each other for
 * the disputed swap.
 */
data class SwapAndDispute(
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
    val disputeRaisedBlockNumber: BigInteger,
    val disputeAgent0: String,
    val disputeAgent1: String,
    val disputeAgent2: String,
    var hasDisputeAgent0Proposed: Boolean,
    var disputeAgent0MakerPayout: BigInteger,
    var disputeAgent0TakerPayout: BigInteger,
    var disputeAgent0ConfiscationPayout: BigInteger,
    var hasDisputeAgent1Proposed: Boolean,
    var disputeAgent1MakerPayout: BigInteger,
    var disputeAgent1TakerPayout: BigInteger,
    var disputeAgent1ConfiscationPayout: BigInteger,
    var hasDisputeAgent2Proposed: Boolean,
    var disputeAgent2MakerPayout: BigInteger,
    var disputeAgent2TakerPayout: BigInteger,
    var disputeAgent2ConfiscationPayout: BigInteger,
    var onChainMatchingProposals: BigInteger,
    var makerReaction: BigInteger,
    var takerReaction: BigInteger,
    var onChainState: BigInteger,
    var hasMakerPaidOut: Boolean,
    var hasTakerPaidOut: Boolean,
    val totalWithoutSpentServiceFees: BigInteger,
    val role: DisputeRole
) {

    var disputeAgent0InterfaceID: ByteArray? = null

    val state = mutableStateOf(DisputeStateAsAgent.NONE)

    var makerCommunicationKey: SymmetricKey? = null
    var takerCommunicationKey: SymmetricKey? = null
    var disputeAgentCommunicationKey: SymmetricKey? = null

    fun toDatabaseSwapAndDispute(): DatabaseSwapAndDispute {
        val encoder = Base64.getEncoder()
        return DatabaseSwapAndDispute(
            id = this.id.toString(),
            isCreated = if (this.isCreated) 1L else 0L,
            requiresFill = if (this.requiresFill) 1L else 0L,
            maker = this.maker,
            makerInterfaceID = encoder.encodeToString(this.makerInterfaceID),
            taker = this.taker,
            takerInterfaceID = encoder.encodeToString(this.takerInterfaceID),
            stablecoin = this.stablecoin,
            amountLowerBound = this.amountLowerBound.toString(),
            amountUpperBound = this.amountUpperBound.toString(),
            securityDepositAmount = this.securityDepositAmount.toString(),
            takenSwapAmount = this.takenSwapAmount.toString(),
            serviceFeeAmount = this.serviceFeeAmount.toString(),
            serviceFeeRate = this.serviceFeeRate.toString(),
            onChainDirection = when (this.direction) {
                OfferDirection.BUY -> "0"
                OfferDirection.SELL -> "1"
            },
            settlementMethod = encoder.encodeToString(this.onChainSettlementMethod),
            protocolVersion = this.protocolVersion.toString(),
            isPaymentSent = if (this.isPaymentSent) 1L else 0L,
            isPaymentReceived = if (this.isPaymentReceived) 1L else 0L,
            hasBuyerClosed = if (this.hasBuyerClosed) 1L else 0L,
            hasSellerClosed = if (this.hasSellerClosed) 1L else 0L,
            disputeRaiser = this.onChainDisputeRaiser.toString(),
            chainID = this.chainID.toString(),
            disputeRaisedBlockNumber = this.disputeRaisedBlockNumber.toString(),
            disputeAgent0 = this.disputeAgent0,
            disputeAgent1 = this.disputeAgent1,
            disputeAgent2 = this.disputeAgent2,
            hasDisputeAgent0Proposed = if(this.hasDisputeAgent0Proposed) 1L else 0L,
            disputeAgent0MakerPayout = this.disputeAgent0MakerPayout.toString(),
            disputeAgent0TakerPayout = this.disputeAgent0TakerPayout.toString(),
            disputeAgent0ConfiscationPayout = this.disputeAgent0ConfiscationPayout.toString(),
            hasDisputeAgent1Proposed = if(this.hasDisputeAgent1Proposed) 1L else 0L,
            disputeAgent1MakerPayout = this.disputeAgent1MakerPayout.toString(),
            disputeAgent1TakerPayout = this.disputeAgent1TakerPayout.toString(),
            disputeAgent1ConfiscationPayout = this.disputeAgent0ConfiscationPayout.toString(),
            hasDisputeAgent2Proposed = if(this.hasDisputeAgent2Proposed) 1L else 0L,
            disputeAgent2MakerPayout = this.disputeAgent2MakerPayout.toString(),
            disputeAgent2TakerPayout = this.disputeAgent2TakerPayout.toString(),
            disputeAgent2ConfiscationPayout = this.disputeAgent2ConfiscationPayout.toString(),
            matchingProposals = this.onChainMatchingProposals.toLong(),
            makerReaction = this.makerReaction.toLong(),
            takerReaction = this.takerReaction.toLong(),
            onChainState = this.onChainState.toLong(),
            hasMakerPaidOut = if(this.hasMakerPaidOut) 1L else 0L,
            hasTakerPaidOut = if(this.hasTakerPaidOut) 1L else 0L,
            totalWithoutSpentServiceFees = this.totalWithoutSpentServiceFees.toString(),
            role = role.asString,
            disputeAgent0InterfaceID = this.disputeAgent0InterfaceID?.let {
                encoder.encodeToString(it)
            },
            state = state.value.asString,
            /*
            These keys and their initialization vectors are always null because this method should ONLY be used before
            these keys are created
             */
            makerCommunicationKey = null,
            mCKInitializationVector = null,
            takerCommunicationKey = null,
            tCKInitializationVector = null,
            disputeAgentCommunicationKey = null,
            dACKInitializationVector = null,
        )
    }

}
