package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.settlement.SettlementMethod
import java.math.BigInteger

/**
 * Validated user-submitted data for taking an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @property takenSwapAmount The stablecoin amount that the user will buy/sell.
 * @property makerSettlementMethod The settlement method, specified and accepted by the maker, by which the user will
 * send/receive payment to/from the maker.
 * @property takerSettlementMethod One of the user's settlement methods which has the same method and currency
 * properties as [makerSettlementMethod], and contains the user's private settlement method details, which will be sent
 * to the maker.
 */
data class ValidatedNewSwapData(
    val takenSwapAmount: BigInteger,
    val makerSettlementMethod: SettlementMethod,
    val takerSettlementMethod: SettlementMethod,
)