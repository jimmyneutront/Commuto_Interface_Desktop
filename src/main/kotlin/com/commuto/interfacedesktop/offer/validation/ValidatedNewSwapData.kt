package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.settlement.SettlementMethod
import java.math.BigInteger

/**
 * Validated user-submitted data for taking an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @property takenSwapAmount The stablecoin amount that the user will buy/sell.
 * @property settlementMethod The settlement method by which the user will send/receive payment to/from the maker.
 */
data class ValidatedNewSwapData(
    val takenSwapAmount: BigInteger,
    val settlementMethod: SettlementMethod
)