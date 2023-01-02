package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * This class represents an [OfferEdited](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeredited)
 * event, emitted by the CommutoSwap smart contract when the price or settlement methods of an open offer is edited.
 *
 * @property offerID The ID of the edited offer, as a [UUID].
 * @property chainID The ID of the blockchain on which this event was emitted.
 * @property transactionHash The hash of the transaction that emitted this event, as a lowercase hex string with a "0x"
 * prefix.
 */
class OfferEditedEvent(val offerID: UUID, val chainID: BigInteger, transactionHash: String) {

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
         * Creates an [OfferEditedEvent] from a [CommutoSwap.OfferEditedEventResponse] and a specified [chainID]
         *
         * @param event The [CommutoSwap.OfferCanceledEventResponse] containing the offer ID of an offer as a
         * [ByteArray].
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [OfferEditedEvent] with an offer ID as a [UUID] derived from [event]'s offer ID [ByteArray]
         * and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.OfferEditedEventResponse, chainID: BigInteger): OfferEditedEvent{
            val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
            val mostSigBits = offerIdByteBuffer.long
            val leastSigBits = offerIdByteBuffer.long
            return OfferEditedEvent(UUID(mostSigBits, leastSigBits), chainID, event.log.transactionHash)
        }
    }

}