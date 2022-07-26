package com.commuto.interfacedesktop.ui

import androidx.compose.runtime.MutableState
import com.commuto.interfacedesktop.offer.OfferTruthSource

/**
 * An interface that a class must adopt in order to act as a single source of truth for open-offer-related data in an
 * application with a graphical user interface.
 * @property isGettingServiceFeeRate Indicates whether the class implementing this interface is currently getting the
 * current service fee rate.
 */
interface UIOfferTruthSource: OfferTruthSource {
    var isGettingServiceFeeRate: MutableState<Boolean>

    /**
     * Should attempt to get the current service fee rate and set the value of [serviceFeeRate] equal to the result.
     */
    fun updateServiceFeeRate()
}