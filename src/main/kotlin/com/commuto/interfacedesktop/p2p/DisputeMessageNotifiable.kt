package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncementAsUserForDispute

/**
 * An interface that a class must implement in order to be notified of dispute-related messages by [P2PService].
 */
interface DisputeMessageNotifiable {
    /**
     * The method called by [P2PService] in order to notify the class implementing this interface of a new
     * [PublicKeyAnnouncementAsUserForDispute].
     *
     * @param message The new [PublicKeyAnnouncementAsUserForDispute] of which the class implementing this interface is
     * being notified and should handle in the implementation of this method.
     */
    suspend fun handlePublicKeyAnnouncementAsUserForDispute(message: PublicKeyAnnouncementAsUserForDispute)
}