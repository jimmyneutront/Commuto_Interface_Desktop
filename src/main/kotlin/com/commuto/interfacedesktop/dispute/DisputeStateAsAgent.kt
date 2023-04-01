package com.commuto.interfacedesktop.dispute

/**
 * Describes the state of a dispute, from the perspective of a dispute agent.
 *
 * @property NONE Indicates that the dispute agent has not yet taken any action related to the dispute.
 * @property SENT_DISPUTE_AGENT_0_PKA Indicates that the user of this interface is the first dispute agent for the
 * corresponding dispute, and that they have created and announced their public key for the dispute.
 * @property CREATED_COMMUNICATION_KEYS Indicates that the user of this interface is the first dispute agent for the
 * corresponding dispute, and that they have created the keys for communication with the maker, taker, and other dispute
 * agents.
 * @property asString A [String] corresponding to a particular case of [DisputeStateAsAgent].
 */
enum class DisputeStateAsAgent {
    NONE,
    SENT_DISPUTE_AGENT_0_PKA,
    CREATED_COMMUNICATION_KEYS;

    val asString: String
        get() = when (this) {
            NONE -> "none"
            SENT_DISPUTE_AGENT_0_PKA -> "sentDisputeAgent0Pka"
            CREATED_COMMUNICATION_KEYS -> "createdCommunicationKeys"
        }

}