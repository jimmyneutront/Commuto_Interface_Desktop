package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.math.BigInteger
import java.util.*

/**
 * An interface that a class must implement in order to act as a single source of truth for offer-related data.
 * @property offers A [SnapshotStateMap] mapping [UUID]s to [Offer]s that should act as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether the class implementing this interface is currently getting the
 * current service fee rate.
 */
interface OfferTruthSource {
    var offers: SnapshotStateMap<UUID, Offer>
    var serviceFeeRate: MutableState<BigInteger?>
    var isGettingServiceFeeRate: MutableState<Boolean>

    /**
     * Should add a new [Offer] to [offers].
     *
     * @param offer The new [Offer] that should be added to [offers].
     */
    fun addOffer(offer: Offer)

    /**
     * Should remove from [offers] all [Offer]s with an ID equal to [id]. There should only be one such [Offer].
     *
     * @param id The ID of the [Offer] to remove.
     */
    fun removeOffer(id: UUID)

    /**
     * Should attempt to get the current service fee rate and set the value of [serviceFeeRate] equal to the result.
     */
    fun updateServiceFeeRate()
}