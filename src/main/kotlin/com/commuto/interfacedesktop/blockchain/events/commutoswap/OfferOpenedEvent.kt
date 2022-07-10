package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * This class represents an [OfferOpened](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened)
 * event, emitted by the CommutoSwap smart contract when a new offer is opened.
 *
 * @property offerID The ID of the newly opened offer, as a [UUID].
 * @property interfaceID The
 * [interface ID](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt#L70) belonging to
 * the maker of the newly opened offer, as a [ByteArray].
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class OfferOpenedEvent(val offerID: UUID, val interfaceID: ByteArray, val chainID: BigInteger) {

    companion object {
        /**
         * Creates an [OfferOpenedEvent] from a [CommutoSwap.OfferOpenedEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.OfferOpenedEventResponse] containing the offer ID and interface ID of a new
         * offer as [ByteArray]s.
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [OfferOpenedEvent] with an offer ID as a [UUID] derived from [event]'s offer ID [ByteArray],
         * [interfaceID] equal to that from [event], and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.OfferOpenedEventResponse, chainID: BigInteger): OfferOpenedEvent{
            val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
            val mostSigBits = offerIdByteBuffer.long
            val leastSigBits = offerIdByteBuffer.long
            return OfferOpenedEvent(UUID(mostSigBits, leastSigBits), event.interfaceId, chainID)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfferOpenedEvent

        if (offerID != other.offerID) return false
        if (!interfaceID.contentEquals(other.interfaceID)) return false
        if (chainID != other.chainID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offerID.hashCode()
        result = 31 * result + interfaceID.contentHashCode()
        result = 31 * result + chainID.hashCode()
        return result
    }

}