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
 * @property openingOfferState Indicates whether we are currently opening an offer, and if so, the point of the
 * [offer opening process](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 * we are currently in.
 * @property openingOfferException The [Exception] that occurred during the offer creation process, or `null` if no such
 * exception has occurred.
 */
interface UIOfferTruthSource: OfferTruthSource {
    var isGettingServiceFeeRate: MutableState<Boolean>
    val openingOfferState: MutableState<OpeningOfferState>
    var openingOfferException: Exception?

    /**
     * Should attempt to get the current service fee rate and set the value of [serviceFeeRate] equal to the result.
     */
    fun updateServiceFeeRate()
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

    fun cancelOffer(offer: Offer, offerCancellationTransaction: RawTransaction?)

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods The new [List] of settlement methods that the maker has selected, indicating that
     * they are willing to use them to send/receive fiat currency for this offer.
     */
    fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>
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