package com.commuto.interfacedesktop.blockchain.events.erc20

/**
 * Describes the purpose for which a token transfer allowance was set, such as opening an offer or filling a swap.
 *
 * @property OPEN_OFFER Indicates that the corresponding token transfer allowance was set in order to open an offer.
 * @property TAKE_OFFER Indicates that the corresponding token transfer allowance was set in order to take an offer.
 * @property FILL_SWAP Indicates that the corresponding token transfer allowance was set in order to fill a
 * maker-as-seller swap.
 * @property asString A human-readable string describing an instance of this type.
 */
enum class TokenTransferApprovalPurpose {
    OPEN_OFFER,
    TAKE_OFFER,
    FILL_SWAP;

    val asString: String
        get() = when(this) {
            OPEN_OFFER -> "openOffer"
            TAKE_OFFER -> "takeOffer"
            FILL_SWAP -> "fillSap"
        }

}