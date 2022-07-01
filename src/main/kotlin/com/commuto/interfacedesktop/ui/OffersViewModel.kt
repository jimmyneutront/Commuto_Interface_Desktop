package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateListOf
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
 * @property offers A mutable state list of [Offer]s that acts as a single source of truth for all offer-related data.
 */
@Singleton
class OffersViewModel @Inject constructor(private val offerService: OfferService): OfferTruthSource {
    init {
        offerService.setOfferTruthSource(this)
    }
    override var offers = mutableStateListOf<Offer>() //Offer.manySampleOffers

    /**
     * Adds a new [Offer] to [offers].
     *
     * @param offer The new [Offer] to be added to [offers].
     */
    override fun addOffer(offer: Offer) {
        offers.add(offer)
    }

    /**
     * Removes all [Offer]s with an ID equal to [id] from [offers]. There should only be one such [Offer].
     *
     * @param id The ID of the [Offer] to remove.
     */
    override fun removeOffer(id: UUID) {
        offers.removeIf { it.id == id }
    }
}