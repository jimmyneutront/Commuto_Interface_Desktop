package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.offer.OfferService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffersViewModel @Inject constructor(offerService: OfferService) {
    val offers = mutableStateOf(offerService.offers)
}