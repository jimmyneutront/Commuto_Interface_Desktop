package com.commuto.interfacedesktop.blockchain

/**
 * Describes what a particular transaction wrapped by a particular [BlockchainTransaction] does. (For example: opens a
 * new offer, fills a swap, etc.)
 *
 * @property CANCEL_OFFER Indicates that an [BlockchainTransaction] cancels an open
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property EDIT_OFFER Indicates that a [BlockchainTransaction] edits an open
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property REPORT_PAYMENT_SENT Indicates that a [BlockchainTransaction] reports that payment has been sent for a
 * swap by calling
 * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent)
 * @property REPORT_PAYMENT_RECEIVED Indicates that a [BlockchainTransaction] reports that payment has been received for
 * a swap by calling
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
 * @property asString A human-readable string describing an instance of this type.
 */
enum class BlockchainTransactionType {
    CANCEL_OFFER,
    EDIT_OFFER,
    REPORT_PAYMENT_SENT,
    REPORT_PAYMENT_RECEIVED;

    val asString: String
        get() = when(this) {
            CANCEL_OFFER -> "cancelOffer"
            EDIT_OFFER -> "editOffer"
            REPORT_PAYMENT_SENT -> "reportPaymentSent"
            REPORT_PAYMENT_RECEIVED -> "reportPaymentReceived"
        }

}