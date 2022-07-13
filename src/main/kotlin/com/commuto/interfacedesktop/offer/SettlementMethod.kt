package com.commuto.interfacedesktop.offer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A settlement method specified by the maker of an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) by which they are willing to
 * send/receive payment.
 *
 * @property currency The human readable symbol of the currency that the maker is willing to send/receive.
 * @property method The method my which the currency specified in [currency] should be sent.
 * @property price The amount of currency specified in [currency] that the maker is willing to exchange for one
 * stablecoin unit.
 */
@Serializable
data class SettlementMethod(
    @SerialName("f") val currency: String,
    @SerialName("p") val method: String,
    @SerialName("m") val price: String,
)