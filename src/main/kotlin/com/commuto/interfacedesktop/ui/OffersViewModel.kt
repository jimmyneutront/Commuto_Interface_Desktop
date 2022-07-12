package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateMapOf
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.offer.OfferTruthSource
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by offer-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing
 * [com.commuto.interfacedesktop.offer.Offer]s from the list of open offers as they are created, canceled and
 * taken.
 * @property offers A mutable state map of [UUID]s to [Offer]s that acts as a single source of truth for all
 * offer-related data.
 */
@Singleton
class OffersViewModel @Inject constructor(private val offerService: OfferService): OfferTruthSource {

    init {
        offerService.setOfferTruthSource(this)
    }

    override var offers = mutableStateMapOf<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }

    /**
     * Adds a new [Offer] to [offers].
     *
     * @param offer The new [Offer] to be added to [offers].
     */
    override fun addOffer(offer: Offer) {
        offers[offer.id] = offer
    }

    /**
     * Removes the [Offer] with an ID equal to [id] from [offers].
     *
     * @param id The ID of the [Offer] to remove.
     */
    override fun removeOffer(id: UUID) {
        offers.remove(id)
    }
}