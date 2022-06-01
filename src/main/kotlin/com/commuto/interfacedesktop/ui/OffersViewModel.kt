package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.offer.Offer

class OffersViewModel {
    val offers = mutableStateOf(Offer.manySampleOffers)
}