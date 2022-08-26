package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement

/**
 * A basic [OfferMessageNotifiable] implementation used to satisfy [P2PService]'s offerService dependency for testing
 * non-offer-related code.
 */
class TestOfferMessageNotifiable: OfferMessageNotifiable {
    /**
     * Does nothing, required to adopt [OfferMessageNotifiable]. Should not be used.
     */
    override suspend fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement) {}
}