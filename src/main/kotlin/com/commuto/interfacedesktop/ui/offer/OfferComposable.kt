package com.commuto.interfacedesktop.ui.offer

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import com.commuto.interfacedesktop.offer.*
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import java.util.*

/**
 * Displays details about a particular [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param offerTruthSource The OffersViewModel that acts as a single source of truth for all offer-related data.
 * @param id The ID of the offer about which this [OfferComposable] is displaying information.
 * @param stablecoinInfoRepo The [StablecoinInformationRepository] that this [Composable] uses to get stablecoin name
 * and currency code information. Defaults to [StablecoinInformationRepository.hardhatStablecoinInfoRepo] if no other
 * value is passed.
 */
@Composable
fun OfferComposable(
    offerTruthSource: UIOfferTruthSource,
    id: UUID?,
    stablecoinInfoRepo: StablecoinInformationRepository =
        StablecoinInformationRepository.hardhatStablecoinInfoRepo,
    focusedOfferComposable: MutableState<FocusedOfferComposable>,
) {

    /**
     * The offer about which this [Composable] displays information.
     */
    val offer = offerTruthSource.offers[id]

    val isShowingTakeOfferDialog = remember { mutableStateOf(false) }

    if (id == null || offer == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Offer is not available.",
            )
        }
    } else if (!offer.isCreated.value && offer.cancelingOfferState.value == CancelingOfferState.NONE) {
        /*
        If isCreated is false and cancelingOfferState.value is NONE, then the offer has been canceled by someone OTHER
        than the user of this interface, and therefore we don't show any offer info, just this message. Otherwise, if
        this offer WAS canceled by the user of this interface, we do show offer info, but relabel the "Cancel Offer"
        button to indicate that the offer has been canceled.
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Offer has been canceled.",
            )
        }
    } else if (offer.isTaken.value && offer.takingOfferState.value == TakingOfferState.NONE) {
        /*
        If isTaken is true and takingOfferState.value is NONE, then the offer has been taken by someone OTHER than the
        user of this interface, and therefore we don't show any offer info, just this message. Otherwise, if this offer
        WAS taken by the user of this interface, we do show offer info, but relabel the "Take Offer" button to indicate
        that the offer has been taken.
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Offer has been canceled.",
            )
        }
    } else {
        val stablecoinInformation = stablecoinInfoRepo.getStablecoinInformation(offer.chainID, offer.stablecoin)
        val settlementMethods = remember { offer.settlementMethods }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(9.dp)
            ) {
                Text(
                    text = "Offer",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Direction:",
                    style =  MaterialTheme.typography.h6,
                )
                DisclosureComposable(
                    header = {
                        Text(
                            text = buildDirectionString(offer.direction, stablecoinInformation),
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    content = {
                        Text(buildDirectionDescriptionString(offer.direction.string, stablecoinInformation))
                    }
                )
                if (!offer.isUserMaker) {
                    Text(
                        text = "If you take this offer, you will:",
                        style =  MaterialTheme.typography.h6,
                    )
                    Text(
                        text = createRoleDescription(
                            offerDirection = offer.direction,
                            stablecoinInformation = stablecoinInformation,
                        ),
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold
                    )
                }
                OfferAmountComposable(
                    stablecoinInformation,
                    offer.amountLowerBound,
                    offer.amountUpperBound,
                    offer.securityDepositAmount,
                )
            }
            Row(
                modifier = Modifier.offset(x = 9.dp)
            ) {
                Column {
                    SettlementMethodsListComposable(stablecoinInformation, settlementMethods)
                }
            }
            Column(
                modifier = Modifier.padding(9.dp)
            ) {
                DisclosureComposable(
                    header = {
                        Text(
                            text = "Service Fee Rate: ${offer.serviceFeeRate.toDouble() / 100.0}%",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    content = {
                        Text(
                            "To take this offer, you must pay a fee equal to ${offer.serviceFeeRate.toDouble()
                                    / 100.0} percent of the amount to be exchanged."
                        )
                    }
                )
                ServiceFeeAmountComposable(
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = offer.amountLowerBound,
                    maximumAmount = offer.amountUpperBound,
                    serviceFeeRate = offer.serviceFeeRate
                )
                DisclosureComposable(
                    header = {
                        Text(
                            text = "Advanced Details",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    content = {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Offer ID: $id"
                            )
                            Text(
                                text = "Chain ID: ${offer.chainID}"
                            )
                        }
                    }
                )
                if (offer.isUserMaker) {
                    if (offer.editingOfferState.value == EditingOfferState.EXCEPTION) {
                        Text(
                            text = offer.editingOfferException?.message ?: "An unknown exception occurred",
                            style =  MaterialTheme.typography.h6,
                            color = Color.Red
                        )
                    }
                    Button(
                        onClick = {
                            focusedOfferComposable.value = FocusedOfferComposable.EditOfferComposable
                        },
                        content = {
                            val editOfferButtonLabel = if (offer.editingOfferState.value != EditingOfferState.EDITING)
                                "Edit Offer" else "Editing Offer"
                            Text(
                                text = editOfferButtonLabel,
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        },
                        border = BorderStroke(3.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor =  Color.Transparent,
                            contentColor = Color.Black,
                        ),
                        elevation = null,
                        modifier = Modifier.width(400.dp),
                    )
                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )
                    if (offer.cancelingOfferState.value == CancelingOfferState.EXCEPTION) {
                        Text(
                            text = offer.cancelingOfferException?.message ?: "An unknown exception occurred",
                            style =  MaterialTheme.typography.h6,
                            color = Color.Red
                        )
                    }
                    Button(
                        onClick = {
                            // Don't let the user try to cancel the offer if it is already canceled or being canceled
                            if (offer.cancelingOfferState.value == CancelingOfferState.NONE  ||
                                offer.cancelingOfferState.value == CancelingOfferState.EXCEPTION) {
                                offerTruthSource.cancelOffer(offer)
                            }
                        },
                        content = {
                            val cancelOfferButtonText: String = when (offer.cancelingOfferState.value) {
                                CancelingOfferState.NONE, CancelingOfferState.EXCEPTION -> {
                                    "Cancel Offer"
                                }
                                CancelingOfferState.CANCELING -> {
                                    "Canceling Offer"
                                }
                                else -> {
                                    "Offer Canceled"
                                }
                            }
                            Text(
                                text = cancelOfferButtonText,
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        },
                        border = BorderStroke(3.dp, Color.Red),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor =  Color.Transparent,
                            contentColor = Color.Red,
                        ),
                        elevation = null,
                        modifier = Modifier.width(400.dp)
                    )
                } else if (offer.state == OfferState.OFFER_OPENED) {
                    if (offer.takingOfferState.value == TakingOfferState.EXCEPTION) {
                        Text(
                            text = offer.takingOfferException?.message ?: "An unknown exception occurred",
                            style =  MaterialTheme.typography.h6,
                            color = Color.Red
                        )
                    }
                    /*
                     We should only display the "Take Offer" button if the user is NOT the maker and if the offer is in
                     the offerOpened state
                      */
                    Button(
                        onClick = {
                            isShowingTakeOfferDialog.value = true
                        },
                        content = {
                            val takeOfferButtonLabel = when (offer.takingOfferState.value) {
                                TakingOfferState.NONE, TakingOfferState.EXCEPTION -> "Take Offer"
                                TakingOfferState.COMPLETED -> "Offer Taken"
                                else -> "Taking Offer"
                            }
                            Text(
                                text = takeOfferButtonLabel,
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        },
                        border = BorderStroke(3.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor =  Color.Transparent,
                            contentColor = Color.Black,
                        ),
                        elevation = null,
                        modifier = Modifier.width(400.dp)
                    )
                }
            }
        }

        if (isShowingTakeOfferDialog.value) {
            Dialog(
                onCloseRequest = {},
                state = DialogState(
                    width = 500.dp,
                    height = 600.dp,
                ),
                title = "Take Offer",
                undecorated = true,
                resizable = false,
                content = {
                    Box(
                        modifier = Modifier
                            .width(600.dp)
                            .height(800.dp)
                    ) {
                        TakeOfferComposable(
                            closeDialog = { isShowingTakeOfferDialog.value = false },
                            offerTruthSource = offerTruthSource,
                            id = id,
                        )
                    }
                },
            )
        }

    }
}

/**
 * Creates the text for the label of the direction-related [DisclosureComposable].
 *
 * @param direction The offer's direction.
 * @param stablecoinInformation A [StablecoinInformation?] for this offer's stablecoin.
 */
fun buildDirectionString(direction: OfferDirection, stablecoinInformation: StablecoinInformation?): String {
    val stablecoinCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val directionJoiner: String = when (direction) {
        OfferDirection.BUY -> {
            "with"
        }
        OfferDirection.SELL -> {
            "for"
        }
    }
    return "${direction.string} $stablecoinCode $directionJoiner fiat"
}

/**
 * Creates the text for the content of the direction-related [DisclosureComposable]
 *
 * @param directionString The offer's direction in human readable format.
 * @param stablecoinInformation A [StablecoinInformation?] for this offer's stablecoin.
 */
fun buildDirectionDescriptionString(directionString: String, stablecoinInformation: StablecoinInformation?): String {
    val stablecoinName = stablecoinInformation?.name ?: "Unknown Stablecoin"
    return "This is a $directionString offer: the maker of this offer wants to ${directionString.lowercase()} " +
            "$stablecoinName in exchange for fiat."
}

/**
 * Creates a role discription string (such as "Buy USDC" or "Sell DAI" for the user, based on the direction of the
 * offer. This should only be displayed if the user is not the maker of the offer.
 *
 * @param offerDirection The [Offer.direction] property of the [Offer].
 * @param stablecoinInformation An optional [StablecoinInformation] for this offer's stablecoin. If this is `null`, this
 * uses the symbol "Unknown Stablecoin".
 *
 * @return A role description.
 */
fun createRoleDescription(offerDirection: OfferDirection, stablecoinInformation: StablecoinInformation?): String {
    val direction = when (offerDirection) {
        /*
        The maker is offering to buy stablecoin, so if the user of this interface takes the offer, they must sell
        stablecoin
         */
        OfferDirection.BUY -> "Sell"
        /*
        The maker is offering to sell stablecoin, so if the user of this interface takes the offer, they must buy
        stablecoin
         */
        OfferDirection.SELL -> "Buy"
    }
    return "$direction ${stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"}"
}

/**
 * Displays a horizontally scrolling list of [SettlementMethodComposable]s.
 *
 * @param stablecoinInformation A [StablecoinInformation?] for this offer's stablecoin.
 * @param settlementMethods The settlement methods to be displayed.
 */
@Composable
fun SettlementMethodsListComposable(
    stablecoinInformation: StablecoinInformation?,
    settlementMethods: SnapshotStateList<SettlementMethod>
) {
    val stablecoinCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    Text(
        text = "Settlement methods:",
        style = MaterialTheme.typography.h6
    )
    if (settlementMethods.size > 0) {
        LazyRow(
            modifier = Modifier.padding(PaddingValues(vertical = 6.dp))
        ) {
            settlementMethods.forEach {
                item {
                    SettlementMethodComposable(stablecoin = stablecoinCode, settlementMethod = it)
                    Spacer(modifier = Modifier.width(9.dp))
                }
            }
        }
    } else {
        Text(
            text = "No settlement methods found",
            style = MaterialTheme.typography.body1
        )
    }
}

/**
 * Displays a card containing information about a settlement method.
 *
 * @param stablecoin The human readable symbol of the stablecoin for which the offer related to these settlement methods
 * has been made.
 * @param settlementMethod The settlement method that this card displays.
 */
@Composable
fun SettlementMethodComposable(stablecoin: String, settlementMethod: SettlementMethod) {
    Column(
        modifier = Modifier
            .border(width = 1.dp, color = Color.Black, CutCornerShape(0.dp))
            .padding(9.dp)
    ) {
        Text(
            text = buildCurrencyDescription(settlementMethod),
            modifier = Modifier.padding(3.dp)
        )
        Text(
            text = buildPriceDescription(settlementMethod, stablecoin),
            modifier = Modifier.padding(3.dp)
        )
    }
}

/**
 * Builds a human readable [String] describing the currency and transfer method, such as "EUR via SEPA" or
 * "USD via SWIFT".
 *
 * @param settlementMethod: THe settlement method for which the currency description should be built.
 */
fun buildCurrencyDescription(settlementMethod: SettlementMethod): String {
    return "${settlementMethod.currency} via ${settlementMethod.method}"
}

/**
 * Builds a human readable [String] describing the price specified for this settlement method, such as "Price: 0.94
 * EUR/DAI" or "Price: 1.00 USD/USDC"
 *
 * @param settlementMethod: The settlement method for which the currency description should be built.
 * @param stablecoin The human readable symbol of the stablecoin for which the offer related to [settlementMethod] has
 * been made.
 */
fun buildPriceDescription(settlementMethod: SettlementMethod, stablecoin: String): String {
    return "Price: ${settlementMethod.price} ${settlementMethod.currency}/$stablecoin"
}

/**
 * A [Composable] that always displays [header], and can expand to display [content] or contract and hide [content] when
 * tapped.
 *
 * @param header A [Composable] that will always be displayed regardless of whether [DisclosureComposable] is expanded
 * or not.
 * @param content A [Composable] that will be displayed below [header] when this is expanded.
 */
@Composable
fun DisclosureComposable(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    var isDisclosureExpanded by remember { mutableStateOf(false) }
    val arrowRotationState by animateFloatAsState(if (isDisclosureExpanded) 180f else 0f)
    Column(
        modifier = Modifier
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearEasing,
                )
            )
            .clickable {
                isDisclosureExpanded = !isDisclosureExpanded
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            header()
            IconButton(
                modifier = Modifier.rotate(arrowRotationState),
                onClick = {
                    isDisclosureExpanded = !isDisclosureExpanded
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Drop Down Arrow"
                )
            }
        }
        if (isDisclosureExpanded) {
            content()
        }
    }
}

/**
 * Displays a preview of [OfferComposable] with id equal that of a sample offer for a known stablecoin: Dai.
 */
@Preview
@Composable
fun PreviewOfferComposableWithDaiOffer() {
    OfferComposable(
        PreviewableOfferTruthSource(),
        Offer.sampleOffers[0].id,
        focusedOfferComposable = mutableStateOf(FocusedOfferComposable.EditOfferComposable),
    )
}

/**
 * Displays a preview of [OfferComposable] with id equal that of a sample offer for an unknown stablecoin.
 */
@Preview
@Composable
fun PreviewOfferComposableWithUnknownStablecoinOffer() {
    OfferComposable(
        PreviewableOfferTruthSource(),
        Offer.sampleOffers[3].id,
        focusedOfferComposable = mutableStateOf(FocusedOfferComposable.EditOfferComposable),
    )
}

/**
 * Displays a preview of [OfferComposable] with id equal to a new random UUID.
 */
@Preview
@Composable
fun PreviewOfferComposableWithRandomUUID() {
    OfferComposable(
        PreviewableOfferTruthSource(),
        UUID.randomUUID(),
        focusedOfferComposable = mutableStateOf(FocusedOfferComposable.EditOfferComposable),
    )
}

/**
 * Displays a preview of [OfferComposable] with id equal to null.
 */
@Preview
@Composable
fun PreviewOfferComposableWithNull() {
    OfferComposable(
        PreviewableOfferTruthSource(),
        null,
        focusedOfferComposable = mutableStateOf(FocusedOfferComposable.EditOfferComposable),
    )
}