package com.commuto.interfacedesktop.offer

/**
 * Indicates whether we are currently approving the transfer of an
 * [ERC20](https://ethereum.org/en/developers/docs/standards/tokens/erc-20/) token. If we are, then this indicates the
 * part of the transfer approval process we are currently in.
 *
 * @property NONE Indicates that we are NOT currently approving a token transfer.
 * @property VALIDATING Indicates that we are currently checking whether a token transfer can be approved.
 * @property SENDING_TRANSACTION Indicates that we are currently sending the transaction that will approve a token
 * transfer.
 * @property AWAITING_TRANSACTION_CONFIRMATION Indicates that we have sent the transaction that will approve a token
 * transfer, and we are waiting for it to be confirmed.
 * @property COMPLETED Indicates that we have approved a token transfer, and the transaction that did so has been
 * confirmed.
 * @property EXCEPTION Indicates that we encountered an exception while approving a token transfer.
 * @property asString Returns a [String] corresponding to a particular case of [TokenTransferApprovalState].
 */
enum class TokenTransferApprovalState {
    NONE,
    VALIDATING,
    SENDING_TRANSACTION,
    AWAITING_TRANSACTION_CONFIRMATION,
    COMPLETED,
    EXCEPTION;

    val asString: String
        get() = when (this) {
            NONE -> "none"
            VALIDATING -> "validating"
            SENDING_TRANSACTION -> "sendingTransaction"
            AWAITING_TRANSACTION_CONFIRMATION -> "awaitingTransactionConfirmation"
            COMPLETED -> "completed"
            EXCEPTION -> "error"
        }

}