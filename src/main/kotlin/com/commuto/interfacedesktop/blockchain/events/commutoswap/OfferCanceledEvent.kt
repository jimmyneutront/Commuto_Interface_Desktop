package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * This class represents an [OfferCanceled](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offercanceled)
 * event, emitted by the CommutoSwap smart contract when an open offer is canceled.
 *
 * @property offerID The ID of the canceled offer, as a [UUID].
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class OfferCanceledEvent(val offerID: UUID, val chainID: BigInteger) {

    companion object {
        /**
         * Creates an [OfferCanceledEvent] from a [CommutoSwap.OfferCanceledEventResponse] and a specified [chainID]
         *
         * @param event The [CommutoSwap.OfferCanceledEventResponse] containing the offer ID of an offer as a
         * [ByteArray].
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [OfferCanceledEvent] with an offer ID as a [UUID] derived from [event]'s offer ID [ByteArray]
         * and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.OfferCanceledEventResponse, chainID: BigInteger): OfferCanceledEvent{
            val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
            val mostSigBits = offerIdByteBuffer.long
            val leastSigBits = offerIdByteBuffer.long
            return OfferCanceledEvent(UUID(mostSigBits, leastSigBits), chainID)
        }
    }

}