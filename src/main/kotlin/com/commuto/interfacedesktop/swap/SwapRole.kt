package com.commuto.interfacedesktop.swap

/**
 * Describes the role of the user of this interface for a particular [Swap].
 *
 * @property MAKER_AND_BUYER Indicates that the user of this interface was the maker of the corresponding swap, and
 * offered to buy stablecoin.
 * @property MAKER_AND_SELLER Indicates that the user of this interface was the maker of the corresponding swap, and
 * offered to sell stablecoin.
 * @property TAKER_AND_BUYER Indicates that the user of this interface was the taker of the corresponding swap, and is
 * buying stablecoin (meaning that the taken offer was a sell offer).
 * @property TAKER_AND_SELLER Indicates that the user of this interface was the taker of the corresponding swap, and is
 * selling stablecoin (meaning that the taken offer was a buy offer).
 * @property asString Returns a [String] corresponding to a particular case of [SwapRole].
 */
enum class SwapRole {
    MAKER_AND_BUYER,
    MAKER_AND_SELLER,
    TAKER_AND_BUYER,
    TAKER_AND_SELLER;

    val asString: String
        get() = when (this) {
            MAKER_AND_BUYER -> "makerAndBuyer"
            MAKER_AND_SELLER -> "makerAndSeller"
            TAKER_AND_BUYER -> "takerAndBuyer"
            TAKER_AND_SELLER -> "takerAndSeller"
        }
}