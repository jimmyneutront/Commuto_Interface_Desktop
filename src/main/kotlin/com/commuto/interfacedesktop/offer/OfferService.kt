package com.commuto.interfacedesktop.offer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfferService @Inject constructor() {
    val offers = Offer.manySampleOffers
}