package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.*

/**
 * An [OfferTruthSource] implementation used for previewing user interfaces.
 *
 * @property offers A mutable state list of [Offer]s that acts as a single source of truth for all offer-related data.
 */
class PreviewableOfferTruthSource: OfferTruthSource {
    override var offers = SnapshotStateList<Offer>().also { it.addAll(Offer.sampleOffers) }

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun addOffer(offer: Offer) {}

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun removeOffer(id: UUID) {}
}