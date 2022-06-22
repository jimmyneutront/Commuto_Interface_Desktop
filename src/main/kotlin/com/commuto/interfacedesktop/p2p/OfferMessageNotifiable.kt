package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncement

/**
 * An interface that a class must implement in order to be notified of offer-related messages by
 * [P2PService].
 */
interface OfferMessageNotifiable {
    /**
     * The method called by [P2PService] in order to notify the class implementing this interface of
     * a new [PublicKeyAnnouncement].
     *
     * @param message The new [PublicKeyAnnouncement] of which the class implementing this interface
     * is being notified and should handle in the implementation of this method.
     */
    fun handlePublicKeyAnnouncement(message: PublicKeyAnnouncement)
}