package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.offer.Offer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffersViewModel @Inject constructor() {
    val offers = mutableStateOf(Offer.manySampleOffers)
}