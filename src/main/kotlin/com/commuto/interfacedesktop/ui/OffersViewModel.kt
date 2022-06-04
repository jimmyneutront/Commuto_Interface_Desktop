package com.commuto.interfacedesktop.ui

import com.commuto.interfacedesktop.offer.OfferService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffersViewModel @Inject constructor(val offerService: OfferService)