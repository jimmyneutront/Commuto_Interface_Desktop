package com.commuto.interfacedesktop.offer

/**
 * Describes the state of an [Offer] according to the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt).
 * The order in which the non-cancellation-related cases of this `enum` are defined is the order in which an offer
 * should move through the states that they represent. The order in which the cancellation-related cases are defined is
 * the order in which an offer being canceled should move through the states that they represent.
 *
 * @property TRANSFER_APPROVAL_FAILED Indicates that the transaction that attempted to call
 * [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in order to
 * open the corresponding offer has failed, and a new one has not yet been created.
 * @property APPROVING_TRANSFER Indicates that this is currently calling
 * [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in order to
 * open the corresponding offer.
 * @property APPROVE_TRANSFER_TRANSACTION_SENT Indicates that the transaction that calls
 * [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) for the
 * corresponding offer has been sent to a connected blockchain node.
 * @property AWAITING_OPENING Indicates that the transaction that calls
 * [approve](https://docs.openzeppelin.com/contracts/2.x/api/token/erc20#IERC20-approve-address-uint256-) in order to
 * approve a token transfer to allow opening the corresponding offer has been confirmed, and the user must now open the
 * offer.
 * @property OPEN_OFFER_TRANSACTION_SENT Indicates that the transaction to call
 * [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) for the corresponding offer has
 * been sent to a connected blockchain node.
 * @property AWAITING_PUBLIC_KEY_ANNOUNCEMENT Indicates that the
 * [OfferOpened event](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offeropened) for the corresponding
 * offer has been detected, so, if the offer's maker is the user of the interface, the public key should now be
 * announced; otherwise this means that we are waiting for the maker to announce their public key.
 * @property OFFER_OPENED Indicates that the corresponding offer has been opened on chain and the maker's public key has
 * been announced.
 * @property TAKEN Indicates that the corresponding offer has been taken.
 * @property CANCELED Indicates that the corresponding offer has been canceled on chain.
 * @property indexNumber An [Int] corresponding to this state's position in a list of possible offer states organized
 * according to the order in which an offer should move through them, with the earliest state at the beginning
 * (corresponding to the integer 0) and the latest state at the end (corresponding to the integer 3).
 * @property asString A [String] corresponding to a particular case of [OfferState].
 */
enum class OfferState {
    TRANSFER_APPROVAL_FAILED,
    APPROVING_TRANSFER,
    APPROVE_TRANSFER_TRANSACTION_SENT,
    AWAITING_OPENING,
    OPEN_OFFER_TRANSACTION_SENT,
    AWAITING_PUBLIC_KEY_ANNOUNCEMENT,
    OFFER_OPENED,
    TAKEN,

    CANCELED;


    val indexNumber: Int
        get() = when (this) {
            TRANSFER_APPROVAL_FAILED -> 0
            APPROVING_TRANSFER -> 1
            APPROVE_TRANSFER_TRANSACTION_SENT -> 2
            AWAITING_OPENING -> 3
            OPEN_OFFER_TRANSACTION_SENT -> 4
            AWAITING_PUBLIC_KEY_ANNOUNCEMENT -> 5
            OFFER_OPENED -> 6
            TAKEN -> 7
            CANCELED -> -1
        }

    val asString: String
        get() = when (this) {
            TRANSFER_APPROVAL_FAILED -> "transferApprovalFailed"
            APPROVING_TRANSFER -> "approvingTransfer"
            APPROVE_TRANSFER_TRANSACTION_SENT -> "approveTransferTransactionSent"
            AWAITING_OPENING -> "awaitingOpening"
            OPEN_OFFER_TRANSACTION_SENT -> "openOfferTransactionSent"
            AWAITING_PUBLIC_KEY_ANNOUNCEMENT -> "awaitingPKAnnouncement"
            OFFER_OPENED -> "offerOpened"
            TAKEN -> "taken"
            CANCELED -> "canceled"
        }

    companion object {
        /**
         * Attempts to create an [OfferState] corresponding to the given [String], or returns `null` if no case
         * corresponds to the given [String].
         *
         * @param string The [String] from which this attempts to create a corresponding [OfferState].
         *
         * @return An [OfferState] corresponding to [string], or `null` if no such [OfferState] exists.
         */
        fun fromString(string: String?): OfferState? {
            when (string) {
                null -> {
                    return null
                }
                "transferApprovalFailed" -> {
                    return TRANSFER_APPROVAL_FAILED
                }
                "approvingTransfer" -> {
                    return APPROVING_TRANSFER
                }
                "approveTransferTransactionSent" -> {
                    return APPROVE_TRANSFER_TRANSACTION_SENT
                }
                "awaitingOpening" -> {
                    return AWAITING_OPENING
                }
                "openOfferTransactionSent" -> {
                    return OPEN_OFFER_TRANSACTION_SENT
                }
                "awaitingPKAnnouncement" -> {
                    return AWAITING_PUBLIC_KEY_ANNOUNCEMENT
                }
                "offerOpened" -> {
                    return OFFER_OPENED
                }
                "taken" -> {
                    return TAKEN
                }
                "canceled" -> {
                    return CANCELED
                }
                else -> {
                    return null
                }
            }
        }
    }

}