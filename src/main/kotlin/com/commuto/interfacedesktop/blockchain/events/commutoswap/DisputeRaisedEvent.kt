package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [DisputeRaised](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#disputeraised) event,
 * emitted by the CommutoSwap smart contract when a swap is disputed.
 *
 * @property swapID The ID of the disputed swap.
 * @property disputeAgent0 The address of the first active dispute agent selected by the dispute raiser.
 * @property disputeAgent1 The address of the second active dispute agent selected by the dispute raiser.
 * @property disputeAgent2 The address of the third active dispute agent selected by the dispute raiser.
 * @property chainID The ID of the blockchain on which this event was emitted.
 * @property transactionHash The hash of the transaction that emitted this event, as a lowercase hex string with a "0x"
 * prefix.
 */
class DisputeRaisedEvent(
    val swapID: UUID,
    val disputeAgent0: String,
    val disputeAgent1: String,
    val disputeAgent2: String,
    val chainID: BigInteger,
    transactionHash: String,
) {

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
         * Creates a [DisputeRaisedEvent] from a [CommutoSwap.DisputeRaisedEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.DisputeRaisedEventResponse] containing the ID of the disputed swap and the
         * addresses of the three selected active dispute agents as [String]s.
         * @param chainID The ID of the blockchain on which this event was emitted.
         *
         * @return A new [DisputeRaisedEvent] with a swap ID as a [UUID] derived from [event]'s swap ID [ByteArray],
         * [disputeAgent0], [disputeAgent1] and [disputeAgent2] equal to that from [event], the specified [chainID], and
         * the transaction hash specified by [event].
         */
        fun fromEventResponse(event: CommutoSwap.DisputeRaisedEventResponse, chainID: BigInteger): DisputeRaisedEvent {
            val swapIdByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIdByteBuffer.long
            val leastSigBits = swapIdByteBuffer.long
            return DisputeRaisedEvent(
                swapID = UUID(mostSigBits, leastSigBits),
                disputeAgent0 = event.disputeAgent0,
                disputeAgent1 = event.disputeAgent1,
                disputeAgent2 = event.disputeAgent2,
                chainID = chainID,
                transactionHash = event.log.transactionHash
            )
        }
    }

}