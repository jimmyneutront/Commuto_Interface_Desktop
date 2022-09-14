package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [PaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#paymentreceived) event,
 * emitted by the CommutoSwap smart contract when the seller in a swap reports that they have received fiat payment.
 *
 * @property swapID The ID of the swap for which payment has been received.
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class PaymentReceivedEvent(val swapID: UUID, val chainID: BigInteger) {

    companion object {
        /**
         * Creates a [PaymentReceivedEvent] from a [CommutoSwap.PaymentReceivedEventResponse] and a specified [chainID].
         *
         * @param event the [CommutoSwap.PaymentReceivedEventResponse] containing the ID of a swap for which receiving
         * payment has been reported.
         * @param chainID The ID of the blockchain on which this event was emitted.
         *
         * @return A new [PaymentReceivedEvent] with a swap ID as a [UUID] derived from [event]'s swap ID [ByteArray]
         * and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.PaymentReceivedEventResponse, chainID: BigInteger):
                PaymentReceivedEvent {
            val swapIDByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIDByteBuffer.long
            val leastSigBits = swapIDByteBuffer.long
            return PaymentReceivedEvent(UUID(mostSigBits, leastSigBits), chainID)
        }
    }
}