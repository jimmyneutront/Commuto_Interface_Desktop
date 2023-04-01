package com.commuto.interfacedesktop.dispute

/**
 * Describes a user's role for a dispute.
 *
 * @property DISPUTE_AGENT_0 Indicates that the user is the first dispute agent for a disputed swap.
 * @property asString A [String] corresponding to a particular case of [DisputeRole].
 */
enum class DisputeRole {
    DISPUTE_AGENT_0;

    val asString: String
        get() = when(this) {
            DISPUTE_AGENT_0 -> "disputeAgent0"
        }

}