package com.commuto.interfacedesktop.offer

import java.util.UUID

/**
 * Represents an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param id The ID that uniquely identifies the offer, as a [UUID].
 * @param direction The direction of the offer, indicating whether the maker is offering to buy
 * stablecoin or sell stablecoin.
 * @param price The price at which the maker is offering to buy/sell stablecoin, as the cost in
 * FIAT per one STBL.
 * @param pair A string of the form "FIAT/STBL", where FIAT is the abbreviation of the fiat currency
 * being exchanged, and STBL is the ticker symbol of the stablecoin being exchanged.
 */
data class Offer(val id: UUID, var direction: String, var price: String, var pair: String) {
    companion object {
        /**
         * A [List] of sample [Offer]s. Used for previewing offer-related Composable functions.
         */
        val sampleOffers = listOf(
            Offer(id = UUID.randomUUID(), direction = "Buy", price = "1.004", pair = "USD/USDT"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "1.002", pair = "GBP/USDC"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
        )

        /**
         * A longer [List] of sample [Offer]s. Used for previewing offer-related Composable
         * functions.
         */
        val manySampleOffers = listOf(
            Offer(id = UUID.randomUUID(), direction = "Buy", price = "1.004", pair = "USD/USDT"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "1.002", pair = "GBP/USDC"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
        )
    }
}