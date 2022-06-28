package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by offer-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing
 * [com.commuto.interfacedesktop.offer.Offer]s from the list of open offers as they are created, canceled and taken.
 * @property offers A mutable state list of [Offer]s that acts as a single source of truth for all offer-related data.
 */
@Singleton
class OffersViewModel @Inject constructor(val offerService: OfferService) {
    init {
        offerService.setOffersTruthSource(this)
    }
    var offers = mutableStateListOf<Offer>() //Offer.manySampleOffers
}