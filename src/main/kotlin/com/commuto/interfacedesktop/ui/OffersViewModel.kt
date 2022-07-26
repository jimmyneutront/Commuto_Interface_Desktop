package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by offer-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing
 * [com.commuto.interfacedesktop.offer.Offer]s from the list of open offers as they are created, canceled and
 * taken.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property offers A mutable state map of [UUID]s to [Offer]s that acts as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether this is currently getting the current service fee rate.
 */
@Singleton
class OffersViewModel @Inject constructor(private val offerService: OfferService): UIOfferTruthSource {

    init {
        offerService.setOfferTruthSource(this)
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override var offers = mutableStateMapOf<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }

    override var serviceFeeRate: MutableState<BigInteger?> = mutableStateOf(null)

    override var isGettingServiceFeeRate = mutableStateOf(false)

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    /**
     * Adds a new [Offer] to [offers].
     *
     * @param offer The new [Offer] to be added to [offers].
     */
    override fun addOffer(offer: Offer) {
        offers[offer.id] = offer
    }

    /**
     * Removes the [Offer] with an ID equal to [id] from [offers].
     *
     * @param id The ID of the [Offer] to remove.
     */
    override fun removeOffer(id: UUID) {
        offers.remove(id)
    }

    /**
     * Gets the current service fee rate via [offerService] in this view model's coroutine scope and sets
     * [serviceFeeRate] equal to the result in the main coroutine dispatcher..
     */
    override fun updateServiceFeeRate() {
        isGettingServiceFeeRate.value = true
        viewModelScope.launch {
            logger.info("updateServiceFeeRate: getting value")
            try {
                val newServiceFeeRate = offerService.getServiceFeeRateAsync().await()
                logger.info("updateServiceFeeRate: got value")
                withContext(Dispatchers.Main) {
                    serviceFeeRate.value = newServiceFeeRate
                }
                logger.info("updateServiceFeeRate: updated value")
            } catch (e: Exception) {
                logger.error("updateServiceFeeRate: got exception during getServiceFeeRate call", e)
            }
            delay(700L)
            withContext(Dispatchers.Main) {
                isGettingServiceFeeRate.value = false
            }
        }
    }

}