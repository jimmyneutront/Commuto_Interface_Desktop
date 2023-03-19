package com.commuto.interfacedesktop.dispute

/**
 * Describes the state of a dispute for a swap.
 *
 * @property NONE Indicates that a dispute has not been raised for the corresponding swap.
 * @property SENT_PKA Indicates that a dispute has been raised for the corresponding swap, the user is the raiser of the
 * dispute, and the user's public key has been announced.
 * @property asString A [String] corresponding to a particular case of [DisputeService].
 */
enum class DisputeState {
    NONE,
    SENT_PKA;

    val asString: String
        get() = when (this) {
            NONE -> "none"
            SENT_PKA -> "sentDisputePka"
        }

}