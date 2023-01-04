package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [BuyerClosed](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#buyerclosed) event, emitted
 * by the CommutoSwap smart contract when the buyer closes of a swap closes the swap.
 *
 * @property swapID The ID of the swap that the buyer has closed.
 * @property chainID The ID of the blockchain on which this event was emitted.
 * @property transactionHash The hash of the transaction that emitted this event, as a lowercase hex string with a "0x"
 * prefix.
 */
class BuyerClosedEvent(val swapID: UUID, val chainID: BigInteger, transactionHash: String) {

    val transactionHash: String

    init {
        this.transactionHash = if (transactionHash.startsWith("0x")) {
            transactionHash.lowercase()
        } else {
            "0x${transactionHash.lowercase()}"
        }
    }

    companion object {
        /**
         * Creates a [BuyerClosedEvent] from a [CommutoSwap.BuyerClosedEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.BuyerClosedEventResponse] containing the ID of the swap as a [ByteArray].
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [BuyerClosedEvent] with a swap ID as a [UUID] derived from [event]'s swapID ID [ByteArray],
         * and the specified [chainID], and the transaction hash specified by [event].
         */
        fun fromEventResponse(event: CommutoSwap.BuyerClosedEventResponse, chainID: BigInteger): BuyerClosedEvent {
            val swapIDByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIDByteBuffer.long
            val leastSigBits = swapIDByteBuffer.long
            return BuyerClosedEvent(UUID(mostSigBits, leastSigBits), chainID, event.log.transactionHash)
        }
    }

}