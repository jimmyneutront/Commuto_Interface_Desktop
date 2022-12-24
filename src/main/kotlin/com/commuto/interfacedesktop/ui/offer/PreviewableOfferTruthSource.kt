package com.commuto.interfacedesktop.ui.offer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OpeningOfferState
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import org.web3j.crypto.RawTransaction
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 * A [UIOfferTruthSource] implementation used for previewing user interfaces.
 *
 * @property offers A [SnapshotStateMap] mapping [UUID]s to [Offer]s, which acts as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether this is currently getting the current service fee rate. This will
 * always be false, this class is used only for previewing user interfaces.
 */
class PreviewableOfferTruthSource: UIOfferTruthSource {
    override var offers = SnapshotStateMap<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }
    override var serviceFeeRate: MutableState<BigInteger?> = mutableStateOf(null)
    override var isGettingServiceFeeRate = mutableStateOf(false)
    override val openingOfferState = mutableStateOf(OpeningOfferState.NONE)
    override var openingOfferException: Exception? = null

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun addOffer(offer: Offer) {}

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun removeOffer(id: UUID) {}

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun updateServiceFeeRate() {}

    /**
     * Does nothing since this class is only used for previewing user interfaces, but is required for implementing
     * [UIOfferTruthSource]
     */
    override fun openOffer(
        chainID: BigInteger,
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>
    ) {}

    /**
     * Does nothing since this class is only used for previewing user interfaces, but is required for implementing
     * [UIOfferTruthSource]
     */
    @Deprecated("Use the new offer pipeline with improved transaction state management")
    override fun cancelOffer(offer: Offer) {}

    /**
     * Not used since this class is for previewing user interfaces, but required for adoption of [UIOfferTruthSource].
     */
    override fun createCancelOfferTransaction(
        offer: Offer,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {}

    /**
     * Not used since this class is for previewing user interfaces, but required for adoption of [UIOfferTruthSource].
     */
    override fun cancelOffer(offer: Offer, offerCancellationTransaction: RawTransaction?) {}

    /**
     * Does nothing since this class is only used for previewing user interfaces, but is required for implementing
     * [UIOfferTruthSource]
     */
    override fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>
    ) {}

    /**
     * Does nothing since this class is only used for previewing user interfaces, but is required for implementing
     * [UIOfferTruthSource]
     */
    override fun takeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
    ) {}

}