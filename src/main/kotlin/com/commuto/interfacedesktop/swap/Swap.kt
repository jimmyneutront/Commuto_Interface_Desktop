package com.commuto.interfacedesktop.swap

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.blockchain.structs.SwapStruct
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.settlement.SettlementMethod
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
 * @param state The initial value of [state].
 * @param role Indicates the interface user's role for this swap.
 *
 * @property onChainDirection Corresponds to an on-chain Swap's `direction` property.
 * @property settlementMethod A [SettlementMethod] derived by deserializing [onChainSettlementMethod].
 * @property state Indicates the current state of this swap, as described in the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * @property fillingSwapState (This property is used only if the user of this interface is the maker of this swap and is
 * selling stablecoin.) This indicates whether we are currently filling this swap, and if so, what part of the swap
 * filling process we are in.
 * @property fillingSwapException (This property is used only if the user of this interface is the maker of this swap
 * and is selling stablecoin.) The [Exception] that we encountered during the swap filling process, or `null` of no such
 * exception has occurred.
 * @property reportingPaymentSentState (This property is used only if the user of this interface is the buyer in this
 * swap.) This indicates whether we are currently reporting that we have sent fiat payment to the seller in this swap,
 * and if so, what part of the payment-sent-reporting process we are in.
 * @property reportingPaymentSentException (This property is used only if the user of this interface is the buyer in
 * this swap.) The [Exception] that we encountered during the reporting-payment-sent process, or `null` if no such
 * exception has occurred.
 * @property reportingPaymentReceivedState (This property is used only if the user of this interface is the seller in
 * this swap.) This indicates whether we are currently reporting that we have received fiat payment from the seller in
 * this swap, and if so, what part of the payment-received-reporting process we are in.
 * @property reportingPaymentReceivedException (This property is used only if the user of this interface is the seller
 * in this swap.) The [Exception] that we encountered during the reporting-payment-received process, or `null` if no
 * such exception has occurred.
 * @property reportingPaymentReceivedState This indicates whether we are currently closing this swap, and if so, what
 * part of the swap-closing process we are in.
 * @property reportingPaymentReceivedException The [Exception] that we encountered during the swap closing process, or
 * `null` if no such exception has occurred.
 * @property closingSwapState This indicates whether we are currently closing this swap, and if so, what part of the
 * swap-closing process we are in.
 * @property closingSwapException The [Exception] that we encountered during the swap closing process, or `null` if no
 * such exception has occurred.
 * @property makerPrivateSettlementMethodData The private settlement method data belonging to the maker of this swap for
 * this swap's settlement method, or `null` if such data does not exist.
 * @property takerPrivateSettlementMethodData The private settlement method data belonging to the taker of this swap for
 * this swap's settlement method, or `null` if such data does not exist.
 */
class Swap(
    val isCreated: Boolean,
    var requiresFill: Boolean,
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
    var isPaymentSent: Boolean,
    var isPaymentReceived: Boolean,
    var hasBuyerClosed: Boolean,
    var hasSellerClosed: Boolean,
    val onChainDisputeRaiser: BigInteger,
    val chainID: BigInteger,
    state: SwapState,
    val role: SwapRole,
) {

    val onChainDirection: BigInteger
    val settlementMethod: SettlementMethod

    val state: MutableState<SwapState> = mutableStateOf(state)

    val fillingSwapState = mutableStateOf(FillingSwapState.NONE)
    var fillingSwapException: Exception? = null

    val reportingPaymentSentState = mutableStateOf(ReportingPaymentSentState.NONE)
    var reportingPaymentSentException: Exception? = null

    val reportingPaymentReceivedState = mutableStateOf(ReportingPaymentReceivedState.NONE)
    var reportingPaymentReceivedException: Exception? = null

    val closingSwapState = mutableStateOf(ClosingSwapState.NONE)
    var closingSwapException: Exception? = null

    var makerPrivateSettlementMethodData: String? = null
    var takerPrivateSettlementMethodData: String? = null

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Swap

        if (isCreated != other.isCreated) return false
        if (requiresFill != other.requiresFill) return false
        if (id != other.id) return false
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
        if (!onChainSettlementMethod.contentEquals(other.onChainSettlementMethod)) return false
        if (protocolVersion != other.protocolVersion) return false
        if (isPaymentSent != other.isPaymentSent) return false
        if (isPaymentReceived != other.isPaymentReceived) return false
        if (hasBuyerClosed != other.hasBuyerClosed) return false
        if (hasSellerClosed != other.hasSellerClosed) return false
        if (onChainDisputeRaiser != other.onChainDisputeRaiser) return false
        if (chainID != other.chainID) return false
        if (role != other.role) return false
        if (onChainDirection != other.onChainDirection) return false
        if (settlementMethod != other.settlementMethod) return false
        if (state != other.state) return false
        if (fillingSwapState != other.fillingSwapState) return false
        if (fillingSwapException != other.fillingSwapException) return false
        if (reportingPaymentSentState != other.reportingPaymentSentState) return false
        if (reportingPaymentSentException != other.reportingPaymentSentException) return false
        if (reportingPaymentReceivedState != other.reportingPaymentReceivedState) return false
        if (reportingPaymentReceivedException != other.reportingPaymentReceivedException) return false
        if (closingSwapState != other.closingSwapState) return false
        if (closingSwapException != other.closingSwapException) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCreated.hashCode()
        result = 31 * result + requiresFill.hashCode()
        result = 31 * result + id.hashCode()
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
        result = 31 * result + onChainSettlementMethod.contentHashCode()
        result = 31 * result + protocolVersion.hashCode()
        result = 31 * result + isPaymentSent.hashCode()
        result = 31 * result + isPaymentReceived.hashCode()
        result = 31 * result + hasBuyerClosed.hashCode()
        result = 31 * result + hasSellerClosed.hashCode()
        result = 31 * result + onChainDisputeRaiser.hashCode()
        result = 31 * result + chainID.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + onChainDirection.hashCode()
        result = 31 * result + settlementMethod.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + fillingSwapState.hashCode()
        result = 31 * result + (fillingSwapException?.hashCode() ?: 0)
        result = 31 * result + reportingPaymentSentState.hashCode()
        result = 31 * result + (reportingPaymentSentException?.hashCode() ?: 0)
        result = 31 * result + reportingPaymentReceivedState.hashCode()
        result = 31 * result + (reportingPaymentReceivedException?.hashCode() ?: 0)
        result = 31 * result + closingSwapState.hashCode()
        result = 31 * result + (closingSwapException?.hashCode() ?: 0)
        return result
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
                amountLowerBound = BigInteger.valueOf(10_000) * BigInteger.TEN.pow(6),
                amountUpperBound = BigInteger.valueOf(20_000) * BigInteger.TEN.pow(6),
                securityDepositAmount = BigInteger.valueOf(2_000) * BigInteger.TEN.pow(6),
                takenSwapAmount = BigInteger.valueOf(15_000) * BigInteger.TEN.pow(6),
                serviceFeeAmount = BigInteger.valueOf(150) * BigInteger.TEN.pow(6),
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