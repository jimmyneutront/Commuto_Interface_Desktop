package com.commuto.interfacedesktop.p2p

import com.commuto.interfacedesktop.p2p.messages.PublicKeyAnnouncementAsUserForDispute

/**
 * A basic [DisputeMessageNotifiable] implementation used to satisfy [P2PService]'s disputeService dependency for
 * testing non-dispute-related code.
 */
class TestDisputeMessageNotifiable: DisputeMessageNotifiable {
    /**
     * Does nothing, required to adopt [DisputeMessageNotifiable]. Should not be used.
     */
    override suspend fun handlePublicKeyAnnouncementAsUserForDispute(message: PublicKeyAnnouncementAsUserForDispute) {}
}