package com.commuto.interfacedesktop.swap

/**
 * Describes the state of a [Swap] according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * The order in which the cases of this `enum` are defined is the order in which a swap should move through the states
 * that they represent.
 *
 * @property TAKING Indicates that [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer)
 * has not yet been called for the offer corresponding to the associated swap.
 * @property TAKE_OFFER_TRANSACTION_BROADCAST Indicates that the transaction to take the offer corresponding to the
 * associated swap has been broadcast.
 */
enum class SwapState {
    TAKING,
    TAKE_OFFER_TRANSACTION_BROADCAST;
}