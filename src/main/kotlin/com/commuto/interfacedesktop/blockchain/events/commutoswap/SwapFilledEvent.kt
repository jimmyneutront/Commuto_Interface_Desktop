package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [SwapFilled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swapfilled) event, emitted by
 * the CommutoSwap smart contract when a swap is filled.
 *
 * @property swapID The ID of the filled swap.
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class SwapFilledEvent(val swapID: UUID, val chainID: BigInteger) {

    companion object {
        /**
         * Creates a [SwapFilledEvent] from a [CommutoSwap.SwapFilledEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.SwapFilledEventResponse] containing the swap ID of a filled swap.
         * @param chainID The ID of the blockchain on which this event was emitted.
         *
         * @return A new [SwapFilledEvent] with a swap ID as a [UUID] derived from [event]'s swap ID [ByteArray] and the
         * specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.SwapFilledEventResponse, chainID: BigInteger): SwapFilledEvent {
            val swapIDByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIDByteBuffer.long
            val leastSigBits = swapIDByteBuffer.long
            return SwapFilledEvent(UUID(mostSigBits, leastSigBits), chainID)
        }

    }

}
