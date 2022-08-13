package com.commuto.interfacedesktop.ui.offer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.commuto.interfacedesktop.offer.*
import com.commuto.interfacedesktop.offer.validation.NewOfferDataValidationException
import com.commuto.interfacedesktop.offer.validation.validateEditedSettlementMethods
import com.commuto.interfacedesktop.offer.validation.validateNewOfferData
import com.commuto.interfacedesktop.offer.validation.validateNewSwapData
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Offers View Model, the single source of truth for all offer-related data. It is observed by offer-related
 * [androidx.compose.runtime.Composable]s.
 *
 * @property offerService The [OfferService] responsible for adding and removing
 * [com.commuto.interfacedesktop.offer.Offer]s from the list of open offers as they are created, canceled and
 * taken.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 * @property offers A mutable state map of [UUID]s to [Offer]s that acts as a single source of truth for all
 * offer-related data.
 * @property serviceFeeRate The current
 * [service fee rate](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-whitepaper.txt) as a
 * percentage times 100, or `null` if the current service fee rate is not known.
 * @property isGettingServiceFeeRate Indicates whether this is currently getting the current service fee rate.
 * @property openingOfferState Indicates whether we are currently opening an offer, and if so, the point of the
 * [offer opening process](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 * we are currently in.
 * @property openingOfferException The [Exception] that occured during the offer creation process, or `null` if no such
 * exception has occured.
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

    override val openingOfferState = mutableStateOf(OpeningOfferState.NONE)

    override var openingOfferException: Exception? = null

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

    /**
     * Sets [openingOfferState]'s value on the Main coroutine dispatcher.
     *
     * @param state The new value to which [openingOfferState]'s value will be set.
     */
    private suspend fun setOpeningOfferState(state: OpeningOfferState) {
        withContext(Dispatchers.Main) {
            openingOfferState.value = state
        }
    }

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
            logger.info("openOffer: validating new offer data")
            setOpeningOfferState(OpeningOfferState.VALIDATING)
            openingOfferState
            try {
                val serviceFeeRateForOffer = serviceFeeRate.value ?: throw NewOfferDataValidationException("Unable " +
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
                    afterObjectCreation = { setOpeningOfferState(OpeningOfferState.STORING) },
                    afterPersistentStorage = { setOpeningOfferState(OpeningOfferState.APPROVING) },
                    afterTransferApproval = { setOpeningOfferState(OpeningOfferState.OPENING) },
                )
                logger.info("openOffer: successfully opened offer")
                setOpeningOfferState(OpeningOfferState.COMPLETED)
            } catch (exception: Exception) {
                logger.error("openOffer: got exception during openOffer call", exception)
                openingOfferException = exception
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
    override fun cancelOffer(offer: Offer) {
        viewModelScope.launch {
            setCancelingOfferState(
                offerID = offer.id,
                state = CancelingOfferState.CANCELING
            )
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
     * Attempts to edit an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) made by the user
     * of this interface.
     *
     * This validates [newSettlementMethods], passes the offer ID and validated settlement methods to
     * [OfferService.editOffer], and then clears [offer]'s [Offer.selectedSettlementMethods].
     *
     * @param offer The [Offer] to be edited.
     * @param newSettlementMethods A [List] of [SettlementMethod]s with which the [Offer] will be edited.
     */
    override fun editOffer(
        offer: Offer,
        newSettlementMethods: List<SettlementMethod>
    ) {
        viewModelScope.launch {
            logger.info("editOffer: editing ${offer.id}")
            setEditingOfferState(
                offerID = offer.id,
                state = EditingOfferState.EDITING
            )
            try {
                logger.info("editOffer: validating edited settlement methods for ${offer.id}")
                val validatedSettlementmethods = validateEditedSettlementMethods(newSettlementMethods)
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
                offer.editingOfferException = exception
                setEditingOfferState(
                    offerID = offer.id,
                    state = EditingOfferState.EXCEPTION
                )
            }
        }
    }

    /**
     * Attempts to take an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
     *
     * @param offer The [Offer] to be taken.
     * @param takenSwapAmount The [BigDecimal] amount of stablecoin that the user wants to buy/sell.
     * @param settlementMethod The [SettlementMethod] that the user has selected to send/receive traditional currency
     * payment.
     */
    override fun takeOffer(
        offer: Offer,
        takenSwapAmount: BigDecimal,
        settlementMethod: SettlementMethod?
    ) {
        viewModelScope.launch {
            logger.info("takeOffer: validating new swap data for ${offer.id}")
            setTakingOfferState(offerID = offer.id, state = TakingOfferState.VALIDATING)
            try {
                // TODO: get the proper stablecoin info repo here
                /*val validatedSwapData =*/ validateNewSwapData(
                    offer = offer,
                    takenSwapAmount = takenSwapAmount,
                    selectedSettlementMethod = settlementMethod,
                    stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
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