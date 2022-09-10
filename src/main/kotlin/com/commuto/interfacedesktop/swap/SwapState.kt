package com.commuto.interfacedesktop.swap

/**
 * Describes the state of a [Swap] according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * The order in which the cases of this `enum` are defined is the order in which a swap should move through the states
 * that they represent.
 *
 * @property TAKING Indicates that [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer)
 * has not yet been called for the offer corresponding to the associated swap.
 * @property TAKE_OFFER_TRANSACTION_BROADCAST Indicates that the transaction to take the offer corresponding to the
 * associated swap has been broadcast.
 * @property AWAITING_TAKER_INFORMATION Indicates that the swap has been taken on-chain, and now the swap taker must
 * send their settlement method information and public key to the maker.
 * @property AWAITING_MAKER_INFORMATION Indicates that the taker has sent their settlement method information and public
 * key to the maker, and now the swap maker must send their settlement method information to the taker.
 * @property AWAITING_FILLING Indicates that the maker has sent their settlement method information to the taker, and
 * that the swap is a maker-as-seller swap and the maker must now fill the swap. If the swap is a maker-as-buyer swap,
 * this is not used.
 * @property FILL_SWAP_TRANSACTION_BROADCAST Indicates that the swap is a maker-as-seller swap and that the transaction
 * to fill the swap has been broadcast.
 * @property AWAITING_PAYMENT_SENT If the swap is a maker-as-seller swap, this indicates that the maker has filled the
 * swap, and the buyer must now send payment for the stablecoin they are purchasing. If the swap is a maker-as-buyer
 * swap, this indicates that the maker has sent their settlement method information to the taker, and the buyer must now
 * send payment for the stablecoin they are purchasing.
 * @property AWAITING_PAYMENT_RECEIVED This indicates that the seller must confirm that they have received fiat currency
 * payment from the buyer.
 * @property asString A [String] corresponding to a particular case of [SwapState].
 */
enum class SwapState {
    TAKING,
    TAKE_OFFER_TRANSACTION_BROADCAST,
    AWAITING_TAKER_INFORMATION,
    AWAITING_MAKER_INFORMATION,
    AWAITING_FILLING,
    FILL_SWAP_TRANSACTION_BROADCAST,
    AWAITING_PAYMENT_SENT,
    REPORT_PAYMENT_SENT_TRANSACTION_BROADCAST,
    AWAITING_PAYMENT_RECEIVED;

    val asString: String
        get() = when (this) {
            TAKING -> "taking"
            TAKE_OFFER_TRANSACTION_BROADCAST -> "takeOfferTxPublished"
            AWAITING_TAKER_INFORMATION -> "awaitingTakerInfo"
            AWAITING_MAKER_INFORMATION -> "awaitingMakerInfo"
            AWAITING_FILLING -> "awaitingFilling"
            FILL_SWAP_TRANSACTION_BROADCAST -> "illSwapTransactionBroadcast"
            AWAITING_PAYMENT_SENT -> "awaitingPaymentSent"
            REPORT_PAYMENT_SENT_TRANSACTION_BROADCAST -> "reportPaymentSentTransactionBroadcast"
            AWAITING_PAYMENT_RECEIVED -> "awaitingPaymentReceived"
        }

}