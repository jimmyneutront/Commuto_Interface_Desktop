package com.commuto.interfacedesktop.offer

import java.util.UUID

data class Offer(val id: UUID, var direction: String, var price: String, var pair: String) {
    companion object {
        val sampleOffers = listOf(
            Offer(id = UUID.randomUUID(), direction = "Buy", price = "1.004", pair = "USD/USDT"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "1.002", pair = "GBP/USDC"),
            Offer(id = UUID.randomUUID(), direction = "Sell", price = "0.997", pair = "PLN/LUSD"),
        )
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