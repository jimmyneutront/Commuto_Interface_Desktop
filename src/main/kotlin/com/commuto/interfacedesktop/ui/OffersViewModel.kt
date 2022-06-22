package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.offer.OfferService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by
 * offer-related [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing
 * [com.commuto.interfacedesktop.offer.Offer]s from the list of open offers as they are
 * created, canceled and taken.
 */
@Singleton
class OffersViewModel @Inject constructor(val offerService: OfferService)