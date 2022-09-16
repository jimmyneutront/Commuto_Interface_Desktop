package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [SellerClosed](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#sellerclosed) event,
 * emitted by the CommutoSwap smart contract when the seller closes of a swap closes the swap.
 *
 * @property swapID The ID of the swap that the seller has closed.
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class SellerClosedEvent(val swapID: UUID, val chainID: BigInteger) {

    companion object {
        /**
         * Creates a [SellerClosedEvent] from a [CommutoSwap.SellerClosedEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.SellerClosedEventResponse] containing the ID of the swap as a [ByteArray].
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [SellerClosedEvent] with a swap ID as a [UUID] derived from [event]'s swapID ID [ByteArray],
         * and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.SellerClosedEventResponse, chainID: BigInteger): SellerClosedEvent {
            val swapIDByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIDByteBuffer.long
            val leastSigBits = swapIDByteBuffer.long
            return SellerClosedEvent(UUID(mostSigBits, leastSigBits), chainID)
        }
    }

}