package com.commuto.interfacedesktop.ui.offer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.offer.*
import com.commuto.interfacedesktop.offer.validation.*
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.web3j.crypto.RawTransaction
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by offer-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing [Offer]s from the list of open offers
 * as they are created, canceled and taken.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property offers A mutable state map of [UUID]s to [Offer]s that acts as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether this is currently getting the current service fee rate.
 * @property approvingTransferToOpenOfferState Indicates whether we are currently approving a token transfer in order to
 * open an offer, and if so, the point of the token transfer approval process that we are currently in.
 * @property approvingTransferToOpenOfferException The [Exception] that occurred during the token transfer approval
 * process, or `null` if no such exception has occurred.
 */
@Singleton
class OffersViewModel @Inject constructor(private val offerService: OfferService): UIOfferTruthSource {

    init {
        offerService.setOfferTruthSource(this)
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override var offers = mutableStateMapOf<UUID, Offer>().also { map ->
        Offer.sampleOffers.map {
            map[it.id] = it
        }
    }

    override var serviceFeeRate: MutableState<BigInteger?> = mutableStateOf(null)

    override var isGettingServiceFeeRate = mutableStateOf(false)

    override val approvingTransferToOpenOfferState = mutableStateOf(TokenTransferApprovalState.NONE)

    override var approvingTransferToOpenOfferException: Exception? = null

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    /**
     * Adds a new [Offer] to [offers].
     *
     * @param offer The new [Offer] to be added to [offers].
     */
    override fun addOffer(offer: Offer) {
        offers[offer.id] = offer
    }

    /**
     * Removes the [Offer] with an ID equal to [id] from [offers].
     *
     * @param id The ID of the [Offer] to remove.
     */
    override fun removeOffer(id: UUID) {
        offers.remove(id)
    }

    // TODO: Remove this once old openOffer method is removed
    /**
     * Sets openingOfferState's value on the Main coroutine dispatcher.
     *
     * @param state The new value to which openingOfferState's value will be set.
     */
    private suspend fun setOpeningOfferState(state: OpeningOfferState) {
        withContext(Dispatchers.Main) {
            //openingOfferState.value = state
        }
    }

    // TODO: Remove this once old cancelOffer method is removed
    /**
     * Sets the [Offer.cancelingOfferState] value of the [Offer] in [offers] with the specified [offerID] on the main
     * coroutine dispatcher.
     *
     * @param offerID The ID of the [Offer] of which to set the [Offer.cancelingOfferState].
     * @param state The value to which the [Offer]'s [Offer.cancelingOfferState] will be set.
     */
    private suspend fun setCancelingOfferState(offerID: UUID, state: CancelingOfferState) {
        withContext(Dispatchers.Main) {
            offers[offerID]?.cancelingOfferState?.value = state
        }
    }

    // TODO: Remove this once old editOffer method is removed
    /**
     * Sets the [Offer.editingOfferState] value of the [Offer] in [offers] with the specified [offerID] on the main
     * coroutine dispatcher.
     *
     * @param offerID The ID of the [Offer] of which to set the [Offer.editingOfferState].
     * @param state The value to which the [Offer]'s [Offer.editingOfferState] will be set.
     */
    private suspend fun setEditingOfferState(offerID: UUID, state: EditingOfferState) {
        withContext(Dispatchers.Main) {
            offers[offerID]?.editingOfferState?.value = state
        }
    }

    /**
     * Sets the [Offer.takingOfferState] value of the [Offer] in [offers] ith the specified [offerID] on the main
     * coroutine dispatcher.
     *
     * @param offerID The ID of the [Offer] of which to set the [Offer.takingOfferState].
     * @param state The value to which the [Offer]'s [Offer.takingOfferState] will be set.
     */
    private suspend fun setTakingOfferState(offerID: UUID, state: TakingOfferState) {
        withContext(Dispatchers.Main) {
            offers[offerID]?.takingOfferState?.value = state
        }
    }

    /**
     * Gets the current service fee rate via [offerService] in this view model's coroutine scope and sets
     * [serviceFeeRate] equal to the result in the main coroutine dispatcher..
     */
    override fun updateServiceFeeRate() {
        isGettingServiceFeeRate.value = true
        viewModelScope.launch {
            logger.info("updateServiceFeeRate: getting value")
            try {
                val newServiceFeeRate = offerService.getServiceFeeRateAsync().await()
                logger.info("updateServiceFeeRate: got value")
                withContext(Dispatchers.Main) {
                    serviceFeeRate.value = newServiceFeeRate
                }
                logger.info("updateServiceFeeRate: updated value")
            } catch (e: Exception) {
                logger.error("updateServiceFeeRate: got exception during getServiceFeeRate call", e)
            }
            delay(700L)
            withContext(Dispatchers.Main) {
                isGettingServiceFeeRate.value = false
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] to approve a token transfer in order to open a new offer.
     *
     * This passes all parameters except [createdTransactionHandler] or [exceptionHandler] to
     * [OfferService.createApproveTokenTransferToOpenOfferTransaction] and then passes the resulting transaction to
     * [createdTransactionHandler] or exception to [exceptionHandler].
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
    override fun createApproveToOpenTransaction(
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            logger.info("createApproveToOpenTransaction: creating...")
            try {
                val createdTransaction = offerService.createApproveTokenTransferToOpenOfferTransaction(
                    stablecoin = stablecoin,
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = minimumAmount,
                    maximumAmount = maximumAmount,
                    securityDepositAmount = securityDepositAmount,
                    direction = direction,
                    settlementMethods = settlementMethods,
                )
                createdTransactionHandler(createdTransaction)
            } catch (exception: Exception) {
                exceptionHandler(exception)
            }
        }
    }

    /**
     * Attempts to approve a token transfer in order to open an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * This clears the [approvingTransferToOpenOfferException] property and sets [approvingTransferToOpenOfferState] to
     * [TokenTransferApprovalState.VALIDATING], and then passes all data to [offerService]'s
     * [OfferService.approveTokenTransferToOpenOffer] function. When this function returns, this sets
     * [approvingTransferToOpenOfferState] to [TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION].
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
     * allowance of the proper amount (determined by the values of the other arguments) of the token specified by
     * [stablecoin].
     */
    override fun approveTokenTransferToOpenOffer(
        chainID: BigInteger,
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>,
        approveTokenTransferToOpenOfferTransaction: RawTransaction?,
    ) {
        this.approvingTransferToOpenOfferException = null
        this.approvingTransferToOpenOfferState.value = TokenTransferApprovalState.VALIDATING
        viewModelScope.launch {
            logger.info("approveTokenTransferToOpenOffer: approving...")
            try {
                offerService.approveTokenTransferToOpenOffer(
                    chainID = chainID,
                    stablecoin = stablecoin,
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = minimumAmount,
                    maximumAmount = maximumAmount,
                    securityDepositAmount = securityDepositAmount,
                    direction = direction,
                    settlementMethods = settlementMethods,
                    approveTokenTransferToOpenOfferTransaction = approveTokenTransferToOpenOfferTransaction
                )
                logger.info("approveTokenTransferToOpenOffer: successfully sent transaction")
                approvingTransferToOpenOfferState.value = TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION
            } catch (exception: Exception) {
                logger.error("approveTokenTransferToOpenOffer: got exception during call", exception)
                approvingTransferToOpenOfferException = exception
                approvingTransferToOpenOfferState.value = TokenTransferApprovalState.EXCEPTION
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] that can open [offer] (which should be made by the user of this interface)
     * by calling [openOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer).
     *
     * This passes [offer] to [OfferService.createOpenOfferTransaction] and then passes the resulting transaction to
     * [createdTransactionHandler] or exception to [exceptionHandler].
     *
     * @param offer The [Offer] to be opened.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createOpenOfferTransaction(
        offer: Offer,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            logger.info("createOpenOfferTransaction: creating for ${offer.id}")
            try {
                val createdTransaction = offerService.createOpenOfferTransaction(offer = offer)
                createdTransactionHandler(createdTransaction)
            } catch (exception: Exception) {
                exceptionHandler(exception)
            }
        }
    }

    /**
     * Attempts to open an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer) made by the user of this
     * interface.
     *
     * This clears the offer-opening-related [Exception] of [offer] and sets [offer]'s [Offer.openingOfferState] to
     * [OpeningOfferState.VALIDATING], and then passes all data to [offerService]'s [OfferService.openOffer] function.
     *
     * @param offer The [Offer] to be opened.
     * @param offerOpeningTransaction An optional [RawTransaction] that can open [offer].
     */
    override fun openOffer(
        offer: Offer,
        offerOpeningTransaction: RawTransaction
    ) {
        offer.openingOfferException = null
        offer.openingOfferState.value = OpeningOfferState.VALIDATING
        viewModelScope.launch {
            logger.info("openOffer: opening ${offer.id}")
            try {
                offerService.openOffer(
                    offer = offer,
                    offerOpeningTransaction = offerOpeningTransaction
                )
                logger.info("openOffer: successfully sent transaction for ${offer.id}")
            } catch (exception: Exception) {
                logger.error("openOffer: got error during call for ${offer.id}", exception)
                withContext(Dispatchers.Main) {
                    offer.openingOfferException = exception
                    offer.openingOfferState.value = OpeningOfferState.EXCEPTION
                }
            }
        }
    }

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
    override fun openOffer(
        chainID: BigInteger,
        stablecoin: String?,
        stablecoinInformation: StablecoinInformation?,
        minimumAmount: BigDecimal,
        maximumAmount: BigDecimal,
        securityDepositAmount: BigDecimal,
        direction: OfferDirection?,
        settlementMethods: List<SettlementMethod>
    ) {
        viewModelScope.launch {
            setOpeningOfferState(OpeningOfferState.VALIDATING)
            logger.info("openOffer: validating new offer data")
            try {
                val serviceFeeRateForOffer = serviceFeeRate.value ?: throw OfferDataValidationException("Unable " +
                        "to determine service fee rate")
                val validatedOfferData = validateNewOfferData(
                    stablecoin = stablecoin,
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = minimumAmount,
                    maximumAmount = maximumAmount,
                    securityDepositAmount = securityDepositAmount,
                    serviceFeeRate = serviceFeeRateForOffer,
                    direction = direction,
                    settlementMethods = settlementMethods
                )
                logger.info("openOffer: opening new offer with validated data")
                offerService.openOffer(
                    offerData = validatedOfferData,
                    afterObjectCreation = { /*setOpeningOfferState(OpeningOfferState.STORING)*/ },
                    afterPersistentStorage = { /*setOpeningOfferState(OpeningOfferState.APPROVING)*/ },
                    afterTransferApproval = { /*setOpeningOfferState(OpeningOfferState.OPENING)*/ },
                )
                logger.info("openOffer: successfully opened offer")
                setOpeningOfferState(OpeningOfferState.COMPLETED)
            } catch (exception: Exception) {
                logger.error("openOffer: got exception during openOffer call", exception)
                //openingOfferException = exception
                setOpeningOfferState(OpeningOfferState.EXCEPTION)
            }
        }
    }

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * @param offer The [Offer] to be canceled.
     */
    @Deprecated("Use the new offer pipeline with improved transaction state management")
    override fun cancelOffer(offer: Offer) {
        viewModelScope.launch {
            logger.info("cancelOffer: canceling offer ${offer.id}")
            try {
                offerService.cancelOffer(
                    offerID = offer.id,
                    chainID = offer.chainID
                )
                logger.info("cancelOffer: successfully canceled offer ${offer.id}")
                setCancelingOfferState(
                    offerID = offer.id,
                    state = CancelingOfferState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.error("cancelOffer: got exception during cancelOffer call for ${offer.id}", exception)
                offer.cancelingOfferException = exception
                setCancelingOfferState(
                    offerID = offer.id,
                    state = CancelingOfferState.EXCEPTION
                )
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] to cancel [offer], which should be made by the user of this interface.
     *
     * This passes [offer]to [OfferService.createCancelOfferTransaction] and then passes the resulting transaction to
     * [createdTransactionHandler] or exception to [exceptionHandler].
     *
     * @param offer The [Offer] to be canceled.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createCancelOfferTransaction(
        offer: Offer,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            logger.info("createCancelOfferTransaction: creating for ${offer.id}")
            try {
                val createdTransaction = offerService.createCancelOfferTransaction(offer = offer)
                withContext(Dispatchers.Main) {
                    createdTransactionHandler(createdTransaction)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exceptionHandler(exception)
                }
            }
        }
    }

    /**
     * Attempts to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the
     * user of this interface.
     *
     * This clears the offer-canceling-related [Exception] of [offer] and sets [offer]'s [Offer.cancelingOfferState] to
     * [CancelingOfferState.VALIDATING], and then passes all data to [offerService]'s [OfferService.cancelOffer]
     * function.
     *
     * @param offer The [Offer] to be canceled.
     * @param offerCancellationTransaction An optional [RawTransaction] that can cancel [offer].
     */
    override fun cancelOffer(offer: Offer, offerCancellationTransaction: RawTransaction?) {
        offer.cancelingOfferException = null
        offer.cancelingOfferState.value = CancelingOfferState.VALIDATING
        viewModelScope.launch {
            logger.info("cancelOffer: canceling offer ${offer.id}")
            try {
                offerService.cancelOffer(
                    offer = offer,
                    offerCancellationTransaction = offerCancellationTransaction
                )
                logger.info("cancelOffer: successfully broadcast transaction for ${offer.id}")
            } catch (exception: Exception) {
                logger.error("cancelOffer: got exception during cancelOffer call for ${offer.id}", exception)
                withContext(Dispatchers.Main) {
                    offer.cancelingOfferException = exception
                    offer.cancelingOfferState.value = CancelingOfferState.EXCEPTION
                }
            }
        }
    }

    /**
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * This validates [newSettlementMethods], passes the offer ID and validated settlement methods to
     * [OfferService.editOffer], and then clears [offer]'s [Offer.selectedSettlementMethods].
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods A [List] of [SettlementMethod]s with which the [Offer] will be edited.
     */
    @Deprecated("Use the new transaction pipeline with improved transaction state management")
    override fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>
    ) {
        viewModelScope.launch {
            /*
            setEditingOfferState(
                offerID = offer.id,
                state = EditingOfferState.EDITING
            )*/
            logger.info("editOffer: editing ${offer.id}")
            try {
                logger.info("editOffer: validating edited settlement methods for ${offer.id}")
                val validatedSettlementmethods = validateSettlementMethods(newSettlementMethods)
                logger.info("editOffer: editing ${offer.id} with validated settlement methods")
                offerService.editOffer(
                    offerID = offer.id,
                    newSettlementMethods = newSettlementMethods
                )
                logger.info("editOffer: successfully edited ${offer.id}")
                withContext(Dispatchers.Main) {
                    offer.updateSettlementMethods(
                        settlementMethods = validatedSettlementmethods
                    )
                }
                // We have successfully edited the offer, so we empty the selected settlement method list.
                offer.selectedSettlementMethods.clear()
                setEditingOfferState(
                    offerID = offer.id,
                    state = EditingOfferState.COMPLETED
                )
            } catch (exception: Exception) {
                logger.info("editOffer: got exception during editOffer call for ${offer.id}", exception)
                withContext(Dispatchers.Main) {
                    offer.editingOfferException = exception
                    offer.editingOfferState.value = EditingOfferState.EXCEPTION
                }
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] to edit [offer] using validated [newSettlementMethods], which should be
     * made by the user of this interface.
     *
     * This validates [newSettlementMethods], passes [offer] to [OfferService.createEditOfferTransaction], and then
     * passes the resulting transaction to [createdTransactionHandler] or exception to [exceptionHandler].
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods A list of [SettlementMethod]s with which [offer] will be edited after they are
     * validated.
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createEditOfferTransaction(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            logger.info("createEditOfferTransaction: creating for ${offer.id}, validating edited settlement methods")
            try {
                val validatedSettlementmethods = validateSettlementMethods(newSettlementMethods)
                val createdTransaction = offerService.createEditOfferTransaction(
                    offer = offer,
                    newSettlementMethods = validatedSettlementmethods
                )
                withContext(Dispatchers.Main) {
                    createdTransactionHandler(createdTransaction)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exceptionHandler(exception)
                }
            }
        }
    }

    /**
     * Attempts to edit [offer] using [offerEditingTransaction], which should have been created using the
     * [SettlementMethod]s in [newSettlementMethods].
     *
     * This clears any offer-editing-related [Exception] of [offer], and sets [offer]'s [Offer.editingOfferState] to
     * [EditingOfferState.VALIDATING], and then passes all data to [offerService]'s `[OfferService.editOffer] function.
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods The [SettlementMethod]s with which [offerEditingTransaction] should have been
     * created.
     * @param offerEditingTransaction An optional [RawTransaction] that can edit [offer] using the [SettlementMethod]s
     * contained in [newSettlementMethods].
     */
    override fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>,
        offerEditingTransaction: RawTransaction?
    ) {
        offer.editingOfferException = null
        offer.editingOfferState.value = EditingOfferState.VALIDATING
        viewModelScope.launch {
            logger.info("editOffer: editing offer ${offer.id}")
            try {
                offerService.editOffer(
                    offer = offer,
                    newSettlementMethods = newSettlementMethods,
                    offerEditingTransaction = offerEditingTransaction,
                )
                logger.info("editOffer: successfully broadcast transaction for ${offer.id}")
            } catch (exception: Exception) {
                logger.error("editOffer: got exception during editOffer call for ${offer.id}", exception)
                offer.editingOfferException = exception
                setEditingOfferState(offerID = offer.id, state = EditingOfferState.EXCEPTION)
            }
        }
    }

    /**
     * Attempts to approve a token transfer in order to take an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * This passes all parameters (except [createdTransactionHandler] or [exceptionHandler]) as well as a
     * [StablecoinInformationRepository] to [OfferService.createApproveTokenTransferToTakeOfferTransaction] and then
     * passes the resulting transaction to [createdTransactionHandler] or exception to [exceptionHandler].
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell. If the offer has
     * lower and upper bound amounts that ARE equal, this parameter will be ignored.
     * @param makerSettlementMethod The [SettlementMethod], belonging to the maker, that the user/taker has selected to
     * send/receive traditional currency payment.
     * @param takerSettlementMethod The [SettlementMethod], belonging to the user/taker, that the user has selected to
     * send/receive traditional currency payment. This must contain the user's valid private settlement method data, and
     * must have method and currency fields matching [makerSettlementMethod].
     * @param createdTransactionHandler A lambda that will accept and handle the created [RawTransaction].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createApproveTokenTransferToTakeOfferTransaction(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
        createdTransactionHandler: (RawTransaction) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            logger.info("createApproveTokenTransferToTakeOfferTransaction: creating for ${offer.id}")
            try {
                // TODO: get proper stablecoin repo here
                val createdTransaction = offerService.createApproveTokenTransferToTakeOfferTransaction(
                    offerToTake = offer,
                    takenSwapAmount = takenSwapAmount,
                    makerSettlementMethod = makerSettlementMethod,
                    takerSettlementMethod = takerSettlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
                )
                withContext(Dispatchers.Main) {
                    createdTransactionHandler(createdTransaction)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exceptionHandler(exception)
                }
            }
        }
    }

    /**
     * Attempts to approve a token transfer in order to take an
     * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * This clears [offer]'s [Offer.approvingToTakeException] property and sets its [Offer.approvingToTakeState] to
     * [TokenTransferApprovalState.VALIDATING], and then passes all data (as well as a
     * [StablecoinInformationRepository]) to [offerService]s [OfferService.approveTokenTransferToTakeOffer] function.
     * When this function returns, this sets [offer]'s [Offer.approvingToTakeState] to
     * [TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION].
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell. If the offer has
     * lower and upper bound amounts that ARE equal, this parameter will be ignored.
     * @param makerSettlementMethod The [SettlementMethod], belonging to the maker, that the user/taker has selected to
     * send/receive traditional currency payment.
     * @param takerSettlementMethod The [SettlementMethod], belonging to the user/taker, that the user has selected to
     * send/receive traditional currency payment. This must contain the user's valid private settlement method data, and
     * must have method and currency fields matching [makerSettlementMethod].
     * @param approveTokenTransferToOpenOfferTransaction An optional [RawTransaction] that can create a token transfer
     * allowance of the proper amount (determined by the values of the other arguments) of the token specified by
     * [offer].
     */
    override fun approveTokenTransferToTakeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
        approveTokenTransferToOpenOfferTransaction: RawTransaction?
    ) {
        offer.approvingToTakeException = null
        offer.approvingToTakeState.value = TokenTransferApprovalState.VALIDATING
        viewModelScope.launch {
            logger.info("approveTokenTransferToTakeOffer: approving for ${offer.id}")
            try {
                // TODO: get proper stablecoin repo here
                offerService.approveTokenTransferToTakeOffer(
                    offerToTake = offer,
                    takenSwapAmount = takenSwapAmount,
                    makerSettlementMethod = makerSettlementMethod,
                    takerSettlementMethod = takerSettlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo,
                    approveTokenTransferToTakeOfferTransaction = approveTokenTransferToOpenOfferTransaction
                )
                logger.info("approveTokenTransferToTakeOffer: successfully")
                withContext(Dispatchers.Main) {
                    offer.approvingToTakeState.value = TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION
                }
            } catch (exception: Exception) {
                logger.error("approveTokenTransferToTakeOffer: got exception during call for ${offer.id}",
                    exception)
                withContext(Dispatchers.Main) {
                    offer.approvingToTakeException = exception
                    offer.approvingToTakeState.value = TokenTransferApprovalState.EXCEPTION
                }
            }
        }
    }

    /**
     * Attempts to create a [RawTransaction] that can take [offer] (which should NOT be made by the user of this
     * interface) by calling [takeOffer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#take-offer).
     *
     * This passes all passed data (along with a [StablecoinInformationRepository]) to [offerService]'s
     * [OfferService.createTakeOfferTransaction] and then passes the resulting transaction and key pair to
     * [createdTransactionAndKeyPairHandler] or exception to [exceptionHandler].
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell. If the offer has
     * lower and upper bound amounts that ARE equal, this parameter will be ignored.
     * @param makerSettlementMethod The [SettlementMethod], belonging to the maker, that the user/taker has selected to
     * send/receive traditional currency payment.
     * @param takerSettlementMethod The [SettlementMethod], belonging to the user/taker, that the user has selected to
     * send/receive traditional currency payment. This must contain the user's valid private settlement method data, and
     * must have method and currency fields matching [makerSettlementMethod].
     * @param createdTransactionAndKeyPairHandler A lambda that will accept and handle the created [RawTransaction] and
     * [KeyPair].
     * @param exceptionHandler A lambda that will accept and handle any exception that occurs during the transaction
     * creation process.
     */
    override fun createTakeOfferTransaction(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
        createdTransactionAndKeyPairHandler: (RawTransaction, KeyPair) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            logger.info("createTakeOfferTransaction: creating for ${offer.id}")
            try {
                // TODO: get proper stablecoin repo here
                val createdTransactionAndKeyPair = offerService.createTakeOfferTransaction(
                    offerToTake = offer,
                    takenSwapAmount = takenSwapAmount,
                    makerSettlementMethod = makerSettlementMethod,
                    takerSettlementMethod = takerSettlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
                )
                withContext(Dispatchers.Main) {
                    createdTransactionAndKeyPairHandler(
                        createdTransactionAndKeyPair.first,
                        createdTransactionAndKeyPair.second
                    )
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    exceptionHandler(exception)
                }
            }
        }
    }

    /**
     * Attempts to take an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#open-offer).
     *
     * This clears the offer-taking-related [Exception] of [offer] and sets [offer]'s [Offer.takingOfferState] to
     * [TakingOfferState.VALIDATING], and then passes all data to [offerService]'s [OfferService.takeOffer] function.
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell. If the offer has
     * lower and upper bound amounts that ARE equal, this parameter will be ignored.
     * @param makerSettlementMethod The [SettlementMethod], belonging to the maker, that the user/taker has selected to
     * send/receive traditional currency payment.
     * @param takerSettlementMethod The [SettlementMethod], belonging to the user/taker, that the user has selected to
     * send/receive traditional currency payment. This must contain the user's valid private settlement method data, and
     * must have method and currency fields matching [makerSettlementMethod].
     * @param keyPair An optional [KeyPair], which serves as the user's/taker's key pair and with which
     * [offerTakingTransaction] should have been created.
     * @param offerTakingTransaction An optional [RawTransaction] that can take [offer].
     */
    override fun takeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
        keyPair: KeyPair?,
        offerTakingTransaction: RawTransaction?,
    ) {
        offer.takingOfferException = null
        offer.approvingToTakeState.value = TokenTransferApprovalState.VALIDATING
        viewModelScope.launch {
            logger.info("takeOffer: taking ${offer.id}")
            try {
                // TODO: get proper stablecoin repo here
                offerService.takeOffer(
                    offerToTake = offer,
                    takenSwapAmount = takenSwapAmount,
                    makerSettlementMethod = makerSettlementMethod,
                    takerSettlementMethod = takerSettlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo,
                    takerKeyPair = keyPair,
                    offerTakingTransaction = offerTakingTransaction,
                )
                logger.info("takeOffer: successfully sent transaction for ${offer.id}")
            } catch (exception: Exception) {
                logger.error("takeOffer: got exception during call for ${offer.id}", exception)
                withContext(Dispatchers.Main) {
                    offer.takingOfferException = exception
                    offer.takingOfferState.value = TakingOfferState.EXCEPTION
                }
            }
        }
    }

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
    override fun takeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        makerSettlementMethod: SettlementMethod?,
        takerSettlementMethod: SettlementMethod?,
    ) {
        viewModelScope.launch {
            setTakingOfferState(offerID = offer.id, state = TakingOfferState.VALIDATING)
            logger.info("takeOffer: validating new swap data for ${offer.id}")
            try {
                // TODO: get the proper stablecoin info repo here
                val validatedSwapData = validateNewSwapData(
                    offer = offer,
                    takenSwapAmount = takenSwapAmount,
                    selectedMakerSettlementMethod = makerSettlementMethod,
                    selectedTakerSettlementMethod = takerSettlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
                )
                //setTakingOfferState(offerID = offer.id, state = TakingOfferState.CHECKING)
                offerService.takeOffer(
                    offerToTake = offer,
                    swapData = validatedSwapData,
                    afterAvailabilityCheck = {
                        /*
                        setTakingOfferState(
                            offerID = offer.id,
                            state = TakingOfferState.CREATING,
                        )*/
                    },
                    afterObjectCreation = {
                        /*
                        setTakingOfferState(
                            offerID = offer.id,
                            state = TakingOfferState.STORING,
                        )*/
                    },
                    afterPersistentStorage = {
                        /*
                        setTakingOfferState(
                            offerID = offer.id,
                            state = TakingOfferState.APPROVING,
                        )*/
                    },
                    afterTransferApproval = {
                        /*
                        setTakingOfferState(
                            offerID = offer.id,
                            state = TakingOfferState.TAKING,
                        )*/
                    }
                )
                logger.info("takeOffer: successfully took offer ${offer.id}")
                setTakingOfferState(offerID = offer.id, state = TakingOfferState.COMPLETED)
            } catch (exception: Exception) {
                logger.error("takeOffer: got error during takeOffer call for ${offer.id}", exception)
                offer.takingOfferException = exception
                setTakingOfferState(offerID = offer.id, state = TakingOfferState.EXCEPTION)
            }
        }
    }

}