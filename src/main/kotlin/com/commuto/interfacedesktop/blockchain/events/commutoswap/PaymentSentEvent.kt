package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * Represents a [PaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#paymentsent) event, emitted
 * by the CommutoSwap smart contract when the buyer in a swap reports that they have sent fiat payment.
 *
 * @property swapID The ID of the swap for which payment has been sent.
 * @property chainID The ID of the blockchain on which this event was emitted.
 * @property transactionHash The hash of the transaction that emitted this event, as a lowercase hex string with a "0x"
 * prefix.
 */
class PaymentSentEvent(val swapID: UUID, val chainID: BigInteger, transactionHash: String) {

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
         * Creates a [PaymentSentEvent] from a [CommutoSwap.PaymentSentEventResponse] and a specified [chainID].
         *
         * @param event the [CommutoSwap.PaymentSentEventResponse] containing the ID of a swap for which sending payment
         * has been reported.
         * @param chainID The ID of the blockchain on which this event was emitted.
         *
         * @return A new [PaymentSentEvent] with a swap ID as a [UUID] derived from [event]'s swap ID [ByteArray] and
         * the specified [chainID], and the transaction hash specified by [event].
         */
        fun fromEventResponse(event: CommutoSwap.PaymentSentEventResponse, chainID: BigInteger): PaymentSentEvent {
            val swapIDByteBuffer = ByteBuffer.wrap(event.swapID)
            val mostSigBits = swapIDByteBuffer.long
            val leastSigBits = swapIDByteBuffer.long
            return PaymentSentEvent(UUID(mostSigBits, leastSigBits), chainID, event.log.transactionHash)
        }
    }
}