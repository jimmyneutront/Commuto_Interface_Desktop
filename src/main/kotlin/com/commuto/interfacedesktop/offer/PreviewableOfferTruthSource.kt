package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * An [OfferTruthSource] implementation used for previewing user interfaces.
 *
 * @property offers A [SnapshotStateMap] mapping [UUID]s to [Offer]s, which acts as a single source of truth for all
 * offer-related data.
 */
class PreviewableOfferTruthSource: OfferTruthSource {
    override var offers = SnapshotStateMap<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun addOffer(offer: Offer) {}

    /**
     * Does nothing, since this class is only used for previewing user interfaces.
     */
    override fun removeOffer(id: UUID) {}
}