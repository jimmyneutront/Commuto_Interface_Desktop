package com.commuto.interfacedesktop.blockchain

/**
 * Describes what a particular transaction wrapped by a particular [BlockchainTransaction] does. (For example: opens a
 * new offer, fills a swap, etc.)
 *
 * @property CANCEL_OFFER Indicates that an [BlockchainTransaction] cancels an open
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 */
enum class BlockchainTransactionType {
    CANCEL_OFFER;
}