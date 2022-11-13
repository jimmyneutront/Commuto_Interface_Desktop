package com.commuto.interfacedesktop.settlement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/**
 * A settlement method specified by the maker of an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) by which they are willing to
 * send/receive payment.
 *
 * @property currency The human readable symbol of the currency that the maker is willing to send/receive.
 * @property method The method my which the currency specified in [currency] should be sent.
 * @property price The amount of currency specified in [currency] that the maker is willing to exchange for one
 * stablecoin unit.
 * @property privateData The private data associated with this settlement method, such as an address or bank account
 * number of the settlement method's owner.
 * @property onChainData This [SettlementMethod] as encoded to UTF-8 bytes, or `null` if this settlement method cannot
 * be encoded.
 */
@Serializable
data class SettlementMethod(
    @SerialName("f") val currency: String,
    @SerialName("m") val method: String,
    @SerialName("p") var price: String,
    @Transient var privateData: String? = null
) {

    val onChainData: ByteArray?
        get() = try {
            Json.encodeToString(this).encodeToByteArray()
        } catch (exception: Exception) {
            null
        }

    companion object {
        /**
         * A [List] of sample [SettlementMethod]s with empty price strings. Used for previewing offer-related Composable
         * functions.
         */
        val sampleSettlementMethodsEmptyPrices = listOf(
            SettlementMethod(
                currency = "EUR",
                price = "",
                method = "SEPA",
                privateData = "Some SEPA data"
            ),
            SettlementMethod(
                currency = "USD",
                price = "",
                method = "SWIFT",
                privateData = "Some SWIFT data"
            ),
            SettlementMethod(
                currency = "BSD",
                price = "",
                method = "SANDDOLLAR",
                privateData = "Some SANDDOLLAR data"
            ),
        )
    }
}