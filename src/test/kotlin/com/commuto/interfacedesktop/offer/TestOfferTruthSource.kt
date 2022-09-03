package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import java.math.BigInteger
import java.util.*

/**
 * A basic [OfferTruthSource] implementation for testing.
 */
class TestOfferTruthSource: OfferTruthSource {
    override var offers = mutableStateMapOf<UUID, Offer>()
    override var serviceFeeRate = mutableStateOf<BigInteger?>(null)
    override fun addOffer(offer: Offer) {
        offers[offer.id] = offer
    }
    override fun removeOffer(id: UUID) {
        offers.remove(id)
    }
}