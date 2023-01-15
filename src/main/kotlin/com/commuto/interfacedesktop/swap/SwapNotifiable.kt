package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.blockchain.BlockchainTransaction
import com.commuto.interfacedesktop.blockchain.BlockchainTransactionException
import com.commuto.interfacedesktop.blockchain.events.commutoswap.*
import com.commuto.interfacedesktop.blockchain.events.erc20.ApprovalEvent
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferService
import java.math.BigInteger
import java.util.*
import javax.inject.Singleton

/**
 * An interface that a class must implement in order to be notified of swap-related events by [BlockchainService] and
 * [OfferService]
 */
@Singleton
interface SwapNotifiable {
    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in order to notify the class
     * implementing this interface that a monitored swap-related [BlockchainTransaction] has failed (either has been
     * confirmed and failed, or has been dropped.)
     *
     * @param transaction The [BlockchainTransaction] wrapping the on-chain transaction that has failed.
     * @param exception A [BlockchainTransactionException] describing why the on-chain transaction has failed.
     */
    suspend fun handleFailedTransaction(transaction: BlockchainTransaction, exception: BlockchainTransactionException)

    /**
     * The function called by [OfferService] in order to send a taker information message for an offer that has been
     * taken by the user of this interface. If [swapID] and [chainID] correspond to an offer that has been taken by the
     * user of this interface, this should send a taker information message and return true. Otherwise, it should not
     * send a message and should return false.
     *
     * @param swapID The ID of the swap for which the structure or class adopting this protocol should announce taker
     * information.
     * @param chainID The ID of the blockchain on which the taken offer exists.
     *
     * @return A [Boolean] indicating whether this sent a taker information message.
     */
    suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger): Boolean

    /**
     * The function called by [OfferService] when an offer made by the user of this interface has been taken.
     *
     * @param takenOffer The [Offer] made by the user of this interface that has been taken and now corresponds to a
     * swap.
     */
    suspend fun handleNewSwap(takenOffer: Offer)

    /**
     * The method called by [com.commuto.interfacedesktop.blockchain.BlockchainService] in order to notify the
     * class implementing this interface of an [ApprovalEvent].
     *
     * @param event The [ApprovalEvent] of which the class implementing this interface is being notified and should
     * handle in the implementation of this method.
     */
    suspend fun handleTokenTransferApprovalEvent(event: ApprovalEvent)

    /**
     * The function called by [BlockchainService] in order to notify the class implementing this interface of a
     * [SwapFilledEvent].
     *
     * @param event The [SwapFilledEvent] of which the class implementing this interface is being notified and should
     * handle in the implementation of this method.
     */
    suspend fun handleSwapFilledEvent(event: SwapFilledEvent)

    /**
     * The function called by [BlockchainService] in order to notify the class implementing this interface of a
     * [PaymentSentEvent].
     *
     * @param event The [PaymentSentEvent] of which the class implementing this interface is being notified and should
     * handle in the implementation of this method.
     */
    suspend fun handlePaymentSentEvent(event: PaymentSentEvent)

    /**
     * The function called by [BlockchainService] in order to notify the class implementing this interface of a
     * [PaymentReceivedEvent].
     *
     * @param event The [PaymentReceivedEvent] of which the class implementing this interface is being notified and
     * should handle in the implementation of this method.
     */
    suspend fun handlePaymentReceivedEvent(event: PaymentReceivedEvent)

    /**
     * The function called by [BlockchainService] in order to notify the class implementing this interface of a
     * [BuyerClosedEvent].
     *
     * @param event The [BuyerClosedEvent] of which the class implementing this interface is being notified and
     * should handle in the implementation of this method.
     */
    suspend fun handleBuyerClosedEvent(event: BuyerClosedEvent)

    /**
     * The function called by [BlockchainService] in order to notify the class implementing this interface of a
     * [SellerClosedEvent].
     *
     * @param event The [SellerClosedEvent] of which the class implementing this interface is being notified and
     * should handle in the implementation of this method.
     */
    suspend fun handleSellerClosedEvent(event: SellerClosedEvent)
}