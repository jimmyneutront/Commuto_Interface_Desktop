package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.offer.OfferDirection
import java.util.*

/**
 * Represents a [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
 *
 * @param id The ID that uniquely identifies this swap, as a [UUID].
 * @param direction The direction of the swap, indicating whether the maker is buying stablecoin or selling stablecoin.
 */
data class Swap(
    val id: UUID,
    val direction: OfferDirection
) {
    companion object {
        /**
         * A [List] of sample [Swap]s. Used for previewing swap-related Composable functions.
         */
        val sampleSwaps = listOf(
            Swap(
                id = UUID.randomUUID(),
                direction = OfferDirection.BUY
            ),
            Swap(
                id = UUID.randomUUID(),
                direction = OfferDirection.BUY
            ),
        )
    }
}