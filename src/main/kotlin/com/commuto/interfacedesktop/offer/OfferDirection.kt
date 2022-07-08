package com.commuto.interfacedesktop.offer

/**
 * Describes the direction of an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer), which
 * specifies whether the maker wants to buy or sell stablecoin.
 *
 * @property BUY Indicates that the maker wants to buy stablecoin.
 * @property SELL Indicates that the maker wants to sell stablecoin.
 * @property string The direction in human-readable format.
 */
enum class OfferDirection {
    BUY,
    SELL;

    val string: String
        get() = when (this) {
            BUY -> {
                "Buy"
            }
            SELL -> {
                "Sell"
            }
        }
}