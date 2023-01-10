package com.commuto.interfacedesktop.ui.offer

import androidx.compose.runtime.MutableState
import com.commuto.interfacedesktop.offer.*
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import org.web3j.crypto.RawTransaction
import java.math.BigDecimal
import java.math.BigInteger

/**
 * An interface that a class must adopt in order to act as a single source of truth for open-offer-related data in an
 * application with a graphical user interface.
 * @property isGettingServiceFeeRate Indicates whether the class implementing this interface is currently getting the
 * current service fee rate.
 * @property approvingTransferToOpenOfferState Indicates whether we are currently opening an offer, and if so, the point
 * of the
 * [offer opening process](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 * we are currently in.
 * @property approvingTransferToOpenOfferException The [Exception] that occurred during the offer creation process, or
 * `null` if no such exception has occurred.
 */
interface UIOfferTruthSource: OfferTruthSource {
    var isGettingServiceFeeRate: MutableState<Boolean>

    /**
     * Should attempt to get the current service fee rate and set the value of [serviceFeeRate] equal to the result.
     */
    fun updateServiceFeeRate()

    val approvingTransferToOpenOfferState: MutableState<TokenTransferApprovalState>
    var approvingTransferToOpenOfferException: Exception?

    /**
     * Attempts to open a new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * @param chainID The ID of the blockchain on which the offer will be created.
     * @param stablecoin The contract address of the stablecoin for which the offer will be created.
     * @param stablecoinInformation A [StablecoinInformation] about the stablecoin for which the offer will be created.
     * @param minimumAmount The minimum [BigDecimal] amount of the new offer.
     * @param maximumAmount The maximum [BigDecimal] amount of the new offer.
     * @param securityDepositAmount The security deposit [BigDecimal] amount for the new offer.
     * @param direction The direction of the new offer.
     * @param settlementMethods The settlement methods of the new offer.
     */
    @Deprecated("Use the new offer pipeline with improved transaction state management")
    fun openOffer(
        chainID: BigInteger,
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>
    )

    /**
     * Attempts to create a [RawTransaction] to approve a token transfer in order to open a new offer.
     *
     * @param stablecoin The contract address of the stablecoin for which the token transfer allowance will be created.
     * @param stablecoinInformation A [StablecoinInformation] about the stablecoin for which token transfer allowance
     * will be created.
     * @param minimumAmount The minimum [BigDecimal] amount of the new offer, for which the token transfer allowance
     * will be created.
     * @param maximumAmount The maximum [BigDecimal] amount of the new offer, for which the token transfer allowance
     * will be created.
     * @param securityDepositAmount The security deposit [BigDecimal] amount for the new offer, for which the token
     * transfer allowance will be created.
     * @param direction The direction of the new offer, for which the token transfer allowance will be created.
     * @param settlementMethods The settlement methods of the new offer, for which the token transfer allowance will be
     * created.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createApproveToOpenTransaction(
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    )

    /**
     * Attempts to approve a token transfer in order to open an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * @param chainID The ID of the blockchain on which the token transfer allowance will be created.
     * @param stablecoin The contract address of the stablecoin for which the token transfer allowance will be created.
     * @param stablecoinInformation A [StablecoinInformation] about the stablecoin for which token transfer allowance
     * will be created.
     * @param minimumAmount The minimum [BigDecimal] amount of the new offer, for which the token transfer allowance
     * will be created.
     * @param maximumAmount The maximum [BigDecimal] amount of the new offer, for which the token transfer allowance
     * will be created.
     * @param securityDepositAmount The security deposit [BigDecimal] amount for the new offer, for which the token
     * transfer allowance will be created.
     * @param direction The direction of the new offer, for which the token transfer allowance will be created.
     * @param settlementMethods The settlement methods of the new offer, for which the token transfer allowance will be
     * created.
     * @param approveTokenTransferToOpenOfferTransaction An optional [RawTransaction] that can create a token transfer
     * allowance of the proper amount (determined by the values of the other arguments) of token specified by
     * [stablecoin].
     */
    fun approveTokenTransferToOpenOffer(
        chainID: BigInteger,
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>,
        approveTokenTransferToOpenOfferTransaction: RawTransaction?,
    )

    /**
     * Attempts to create a [RawTransaction] that can open [offer] (which should be made by the user of this interface)
     * by calling [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer).
     *
     * @param offer The [Offer] to be opened.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createOpenOfferTransaction(
        offer: Offer,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    )

    /**
     * Attempts to open an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) made by the user of this
     * interface.
     *
     * @param offer The [Offer] to be opened.
     * @param offerOpeningTransaction An optional [RawTransaction] that can open [offer].
     */
    fun openOffer(
        offer: Offer,
        offerOpeningTransaction: RawTransaction
    )

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * @param offer The [Offer] to be canceled.
     */
    @Deprecated("Use the new offer pipeline with improved transaction state management")
    fun cancelOffer(
        offer: Offer
    )

    /**
     * Attempts to create a [RawTransaction] to cancel [offer], which should be made by the user of this interface.
     *
     * @param offer The [Offer] to be canceled.
     * @param createdTransactionHandler An escaping closure that will accept and handle the created [RawTransaction].
     * @param exceptionHandler An escaping closure that will accept and handle any error that occurs during the
     * transaction creation process.
     */
    fun createCancelOfferTransaction(
        offer: Offer,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    )

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * @param offer The [Offer] to be canceled.
     * @param offerCancellationTransaction An optional [RawTransaction] that can cancel [offer].
     */
    fun cancelOffer(offer: Offer, offerCancellationTransaction: RawTransaction?)

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods The new [List] of settlement methods that the maker has selected, indicating that
     * they are willing to use them to send/receive fiat currency for this offer.
     */
    @Deprecated("Use the new offer pipeline with improved transaction state management")
    fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>
    )

    /**
     * Attempts to create a [RawTransaction] to edit [offer] using validated [newSettlementMethods], which should be
     * made by the user of this interface.
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods A list of [SettlementMethod]s with which [offer] will be edited after they are
     * validated.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    fun createEditOfferTransaction(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    )

    /**
     * Attempts to edit [offer] using [offerEditingTransaction], which should have been created using the
     * [SettlementMethod]s in [newSettlementMethods].
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods The [SettlementMethod]s with which [offerEditingTransaction] should have been
     * created.
     * @param offerEditingTransaction An optional [RawTransaction] that can edit [offer] using the [SettlementMethod]s
     * contained in [newSettlementMethods].
     */
    fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>,
        offerEditingTransaction: RawTransaction?
    )

    /**
     * Attempts to take an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell. If the offer has
     * lower and upper bound amounts that ARE equal, this parameter will be ignored.
     * @param makerSettlementMethod The [SettlementMethod], belonging to the maker, that the user/taker has selected to
     * send/receive traditional currency payment.
     * @param takerSettlementMethod The [SettlementMethod], belonging to the user/taker, that the user has selected to
     * send/receive traditional currency payment. This must contain the user's valid private settlement method data, and
     * must have method and currency fields matching [makerSettlementMethod].
     */
    fun takeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
    )

}