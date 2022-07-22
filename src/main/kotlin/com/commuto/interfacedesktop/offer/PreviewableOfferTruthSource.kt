package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.math.BigInteger
import java.util.*

/**
 * An [OfferTruthSource] implementation used for previewing user interfaces.
 *
 * @property offers A [SnapshotStateMap] mapping [UUID]s to [Offer]s, which acts as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether this is currently getting the current service fee rate. This will
 * always be false, this this class is used only for previewing user interfaces.
 */
class PreviewableOfferTruthSource: OfferTruthSource {
    override var offers = SnapshotStateMap<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }
    override var serviceFeeRate: MutableState<BigInteger?> = mutableStateOf(null)
    override var isGettingServiceFeeRate = mutableStateOf(false)

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
}