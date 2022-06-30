package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * An interface that a class must implement in order to act as a single source of truth for offer-related data.
 * @property offers A [SnapshotStateList] of [Offer]s that should act as a single source of truth for all offer-related
 * data.
 */
interface OfferTruthSource {
    var offers: SnapshotStateList<Offer>

    /**
     * Should add a new [Offer] to [offers].
     *
     * @param offer The new [Offer] that shoudl be added to [offers].
     */
    fun addOffer(offer: Offer)
}