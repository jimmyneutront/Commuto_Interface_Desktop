package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

/**
 * This class represents an [OfferTaken](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offertaken)
 * event, emitted by the CommutoSwap smart contract when an open offer is taken.
 *
 * @property offerID The ID of the taken offer, as a [UUID].
 * @property takerInterfaceID The
 * [interface ID](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt#L70) belonging to
 * the taker of the offer, as a [ByteArray].
 * @property chainID The ID of the blockchain on which this event was emitted.
 */
data class OfferTakenEvent(val offerID: UUID, val takerInterfaceID: ByteArray, val chainID: BigInteger) {

    companion object {
        /**
         * Creates an [OfferTakenEvent] from a [CommutoSwap.OfferTakenEventResponse] and a specified [chainID].
         *
         * @param event The [CommutoSwap.OfferTakenEventResponse] containing the offer ID of a taken offer and and the
         * interface ID of the taker of the offer as [ByteArray]s.
         * @param chainID: The ID of the blockchain on which this event was emitted.
         *
         * @return A new [OfferTakenEvent] with an offer ID as a [UUID] derived from [event]'s offer ID [ByteArray],
         * [takerInterfaceID] equal to that from [event], and the specified [chainID].
         */
        fun fromEventResponse(event: CommutoSwap.OfferTakenEventResponse, chainID: BigInteger): OfferTakenEvent{
            val offerIdByteBuffer = ByteBuffer.wrap(event.offerID)
            val mostSigBits = offerIdByteBuffer.long
            val leastSigBits = offerIdByteBuffer.long
            return OfferTakenEvent(UUID(mostSigBits, leastSigBits), event.takerInterfaceId, chainID)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfferOpenedEvent

        if (offerID != other.offerID) return false
        if (!takerInterfaceID.contentEquals(other.interfaceID)) return false
        if (chainID != other.chainID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offerID.hashCode()
        result = 31 * result + takerInterfaceID.contentHashCode()
        result = 31 * result + chainID.hashCode()
        return result
    }

}