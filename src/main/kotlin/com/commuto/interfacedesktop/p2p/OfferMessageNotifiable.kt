package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement

interface OfferMessageNotifiable {
    fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement)
}