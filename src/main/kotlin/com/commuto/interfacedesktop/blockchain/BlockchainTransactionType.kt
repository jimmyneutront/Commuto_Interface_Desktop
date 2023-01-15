package com.commuto.interfacedesktop.blockchain

/**
 * Describes what a particular transaction wrapped by a particular [BlockchainTransaction] does. (For example: opens a
 * new offer, fills a swap, etc.)
 *
 * @property APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER Indicates that a [BlockchainTransaction] approves a token transfer
 * by calling [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in
 * order to open an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property OPEN_OFFER Indicates that a [BlockchainTransaction] opens an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) by calling
 * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer).
 * @property CANCEL_OFFER Indicates that an [BlockchainTransaction] cancels an open
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property EDIT_OFFER Indicates that a [BlockchainTransaction] edits an open
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER Indicates that a [BlockchainTransaction] approves a token transfer by
 * calling [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in
 * order to take an open [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 * @property TAKE_OFFER Indicates that a [BlockchainTransaction] takes an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) by calling
 * [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer).
 * @property APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP Indicates that a [BlockchainTransaction] approves a token transfer by
 * calling [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in
 * order to fill a maker as seller [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) made by
 * the user of this interface.
 * @property TAKE_OFFER Indicates that a [BlockchainTransaction] fills a maker-as-seller
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap) made by the user of this interface by
 * calling [fillSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#fill-swap).
 * @property REPORT_PAYMENT_SENT Indicates that a [BlockchainTransaction] reports that payment has been sent for a
 * swap by calling
 * [reportPaymentSent](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-sent)
 * @property REPORT_PAYMENT_RECEIVED Indicates that a [BlockchainTransaction] reports that payment has been received for
 * a swap by calling
 * [reportPaymentReceived](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#report-payment-received)
 * @property CLOSE_SWAP Indicates that a [BlockchainTransaction] reports that payment has been received for
 * a swap by calling
 * [closeSwap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#close-swap)
 * @property asString A human-readable string describing an instance of this type.
 */
enum class BlockchainTransactionType {
    APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER,
    OPEN_OFFER,
    CANCEL_OFFER,
    EDIT_OFFER,
    APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER,
    TAKE_OFFER,
    APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP,
    FILL_SWAP,
    REPORT_PAYMENT_SENT,
    REPORT_PAYMENT_RECEIVED,
    CLOSE_SWAP;

    val asString: String
        get() = when(this) {
            APPROVE_TOKEN_TRANSFER_TO_OPEN_OFFER -> "approveTokenTransferToOpenOffer"
            OPEN_OFFER -> "openOffer"
            CANCEL_OFFER -> "cancelOffer"
            EDIT_OFFER -> "editOffer"
            APPROVE_TOKEN_TRANSFER_TO_TAKE_OFFER -> "approveTokenTransferToTakeOffer"
            TAKE_OFFER -> "takeOffer"
            APPROVE_TOKEN_TRANSFER_TO_FILL_SWAP -> "approveTokenTransferToFillSwap"
            FILL_SWAP -> "fillSwap"
            REPORT_PAYMENT_SENT -> "reportPaymentSent"
            REPORT_PAYMENT_RECEIVED -> "reportPaymentReceived"
            CLOSE_SWAP -> "closeSwap"
        }

}