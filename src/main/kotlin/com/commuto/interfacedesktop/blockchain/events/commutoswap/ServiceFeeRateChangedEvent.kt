package com.commuto.interfacedesktop.blockchain.events.commutoswap

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import java.math.BigInteger

/**
 * This class represents a
 * [ServiceFeeRateChanged](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#servicefeeratechanged) event,
 * emitted by the CommutoSwap smart contract when the service fee rate is changed.
 *
 * @property newServiceFeeRate The new service fee rate, as a percentage times 100.
 */
data class ServiceFeeRateChangedEvent(val newServiceFeeRate: BigInteger) {

    companion object {
        /**
         * Creates a [ServiceFeeRateChangedEvent] from a [CommutoSwap.ServiceFeeRateChangedEventResponse].
         *
         * @param event The [CommutoSwap.ServiceFeeRateChangedEventResponse] containing the new service fee rate.
         *
         * @return A new [ServiceFeeRateChangedEvent] with [newServiceFeeRate] equal to that specified in [event].
         */
        fun fromEventResponse(event: CommutoSwap.ServiceFeeRateChangedEventResponse): ServiceFeeRateChangedEvent {
            return ServiceFeeRateChangedEvent(event.newServiceFeeRate)
        }
    }

}