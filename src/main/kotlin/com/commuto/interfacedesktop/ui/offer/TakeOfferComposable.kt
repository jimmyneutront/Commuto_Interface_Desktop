package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.CancelingOfferState
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.SettlementMethod
import com.commuto.interfacedesktop.offer.TakingOfferState
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import java.math.BigDecimal
import java.util.*

/**
 * Allows the user to specify a stablecoin amount, select a settlement method and take an
 * [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer). This should be contained by a
 * dialog.
 *
 * @param closeDialog The lambda that can close the dialog in which this [Composable] is displayed.
 * @param offerTruthSource The OffersViewModel that acts as a single source of truth for all offer-related data.
 * @param id The ID of the offer about which this [TakeOfferComposable] displays information.
 * @param stablecoinInfoRepo The [StablecoinInformationRepository] that this [Composable] uses to get stablecoin name
 * and currency code information. Defaults to [StablecoinInformationRepository.hardhatStablecoinInfoRepo] if no
 * other value is passed.
 */
@Composable
fun TakeOfferComposable(
    closeDialog: () -> Unit,
    offerTruthSource: UIOfferTruthSource,
    id: UUID?,
    stablecoinInfoRepo: StablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
) {

    val offer = offerTruthSource.offers[id]

    /**
     * The amount of stablecoin that the user has indicated they want to buy/sell. If the minimum and maximum amount of
     * the offer this [Composable] represents are equal, this will not be used.
     */
    val specifiedStablecoinAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * specifiedStablecoinAmount as a [String].
     */
    val specifiedStablecoinAmountString = remember { mutableStateOf("0.00") }

    /**
     * The settlement method by which the user has chosen to send/receive traditional currency payment, or `null` if the
     * user has not made such a selection.
     */
    val selectedSettlementMethod = remember { mutableStateOf<SettlementMethod?>(null) }

    if (offer == null) {
        TakeOfferUnavailableComposable(
            message = "This Offer is unavailable.",
            closeDialog = closeDialog
        )
    } else if (!offer.isCreated.value && offer.cancelingOfferState.value == CancelingOfferState.NONE) {
        /*
        If isCreated is false and cancelingOfferState.value is NONE, then the offer has been canceled by someone OTHER
        than the user of this interface, and therefore we don't show any offer info, just this message. Otherwise, if
        this offer WAS canceled by the user of this interface, we do show offer info, but relabel the "Cancel Offer"
        button to indicate that the offer has been canceled.
         */
        TakeOfferUnavailableComposable(
            message = "This Offer has been canceled.",
            closeDialog = closeDialog
        )
    } else if (offer.isTaken.value && offer.takingOfferState.value == TakingOfferState.NONE) {
        /*
        If isTaken is true and takingOfferState.value is NONE, then the offer has been taken by someone OTHER than the
        user of this interface, and therefore we don't show any offer info, just this message. Otherwise, if this offer
        WAS taken by the user of this interface, we do show offer info, but relabel the "Take Offer" button to indicate
        that the offer has been taken.
         */
        TakeOfferUnavailableComposable(
            message = "This Offer has been taken.",
            closeDialog = closeDialog
        )
    } else {
        val stablecoinInformation = stablecoinInfoRepo.getStablecoinInformation(
            chainID = offer.chainID, contractAddress = offer.stablecoin
        )
        val closeSheetButtonLabel = if (offer.takingOfferState.value == TakingOfferState.COMPLETED) "Done" else "Cancel"
        Column(
            modifier = Modifier
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Take ${offer.direction.string} Offer",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = {
                        closeDialog()
                    },
                    content = {
                        Text(
                            text = closeSheetButtonLabel,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor =  Color.Transparent,
                        contentColor = Color.Black,
                    ),
                    elevation = null,
                )
            }
            OfferAmountComposable(
                stablecoinInformation = stablecoinInformation,
                min = offer.amountLowerBound,
                max = offer.amountUpperBound,
                securityDeposit = offer.securityDepositAmount,
            )
            if (offer.amountLowerBound != offer.amountUpperBound) {
                /*
                If the upper and lower bound of the offer amount are NOT equal, then we want to display a single service
                fee amount based on the amount that the user has specified.
                 */
                Text(
                    text = "Enter an Amount:"
                )
                StablecoinAmountFieldComposable(
                    amountString = specifiedStablecoinAmountString,
                    amount = specifiedStablecoinAmount,
                )
                ServiceFeeAmountComposable(
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = specifiedStablecoinAmount.value,
                    maximumAmount = specifiedStablecoinAmount.value,
                    serviceFeeRate = offer.serviceFeeRate
                )
            } else {
                /*
                If the upper and lower bound of the offer amount are equal, then only one service fee amount should be
                displayed and we can pass the lower bound amount as both the minimum and the maximum to this Composable
                 */
                ServiceFeeAmountComposable(
                    stablecoinInformation = stablecoinInformation,
                    minimumAmount = offer.amountLowerBound,
                    maximumAmount = offer.amountLowerBound,
                    serviceFeeRate = offer.serviceFeeRate
                )
            }
            val settlementMethodHeader = if (offer.settlementMethods.size == 1) {
                "Settlement Method"
            } else {
                "Settlement Methods"
            }
            Text(
                text = settlementMethodHeader,
                style =  MaterialTheme.typography.h6,
            )
            ImmutableSettlementMethodSelector(
                settlementMethods = offer.settlementMethods,
                selectedSettlementMethod = selectedSettlementMethod,
                stablecoinCurrencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
            )
            if (offer.takingOfferState.value != TakingOfferState.NONE &&
                offer.takingOfferState.value != TakingOfferState.EXCEPTION) {
                Text(
                    text = offer.takingOfferState.value.description,
                    style =  MaterialTheme.typography.h6,
                )
            }
            if (offer.takingOfferState.value == TakingOfferState.EXCEPTION) {
                Text(
                    text = offer.takingOfferException?.message ?: "An unknown exception occurred",
                    style =  MaterialTheme.typography.h6,
                    color = Color.Red
                )
            }
            val takeOfferButtonOutlineColor = when (offer.takingOfferState.value) {
                TakingOfferState.NONE, TakingOfferState.EXCEPTION -> Color.Black
                else -> Color.Gray
            }
            Button(
                onClick = {
                    if (offer.takingOfferState.value == TakingOfferState.NONE ||
                        offer.takingOfferState.value == TakingOfferState.EXCEPTION) {
                        offerTruthSource.takeOffer(
                            offer = offer,
                            takenSwapAmount = specifiedStablecoinAmount.value,
                            settlementMethod = selectedSettlementMethod.value
                        )
                    }
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
                border = BorderStroke(3.dp, takeOfferButtonOutlineColor),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                elevation = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Used to display an error message when an offer cannot be taken for some reason. This should only be used in
 * [TakeOfferComposable].
 *
 * @param message A message explaining why the offer cannot be taken.
 * @param closeDialog A lambda that will close the dialog in which this [Composable] is presented.
 */
@Composable
fun TakeOfferUnavailableComposable(
    message: String,
    closeDialog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Take Offer",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = {
                    closeDialog()
                },
                content = {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                elevation = null,
            )
        }
        Text(
            text = message,
        )
    }
}

/**
 * Displays a vertical list of [ImmutableSettlementMethodCard]s, one for each of this offer's settlement methods. Only
 * one [ImmutableSettlementMethodCard] can be selected at a time. When such a card is selected,
 * [selectedSettlementMethod] is set equal to the [SettlementMethod] that card represents.
 *
 * @param settlementMethods The offer's [SettlementMethod]s.
 * @param selectedSettlementMethod The currently selected [SettlementMethod] method, or `null` if no [SettlementMethod]
 * is currently selected.
 * @param stablecoinCurrencyCode The currency code of the offer's stablecoin.
 */
@Composable
fun ImmutableSettlementMethodSelector(
    settlementMethods: SnapshotStateList<SettlementMethod>,
    selectedSettlementMethod: MutableState<SettlementMethod?>,
    stablecoinCurrencyCode: String,
) {
    Column {
        for (settlementMethod in settlementMethods) {
            val color = if (settlementMethod == selectedSettlementMethod.value) Color.Green else Color.Black
            Button(
                onClick = {
                    selectedSettlementMethod.value = settlementMethod
                },
                content = {
                    ImmutableSettlementMethodCard(
                        settlementMethod = settlementMethod,
                        color = color,
                        stablecoinCurrencyCode = stablecoinCurrencyCode,
                    )
                },
                border = BorderStroke(1.dp, color),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = color
                ),
                elevation = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            )
            Spacer(
                modifier = Modifier.height(5.dp)
            )
        }
    }
}

/**
 * Displays a card containing settlement method information that cannot be edited.
 *
 * @param settlementMethod The [SettlementMethod] that this card represents.
 * @param color The color of the text inside of this card.
 * @param stablecoinCurrencyCode The currency code of the currently selected stablecoin.
 */
@Composable
fun ImmutableSettlementMethodCard(
    settlementMethod: SettlementMethod,
    color: Color,
    stablecoinCurrencyCode: String,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .padding(PaddingValues(horizontal = 10.dp))
            .fillMaxWidth()
    ) {
        Text(
            text = buildCurrencyDescription(settlementMethod),
            style =  MaterialTheme.typography.h6,
            color = color,
            modifier = Modifier.padding(PaddingValues(top = 10.dp))
        )
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Text(
            text = buildOpenOfferPriceDescription(settlementMethod, stablecoinCurrencyCode),
            style =  MaterialTheme.typography.h6,
            color = color,
            modifier = Modifier.padding(PaddingValues(bottom = 10.dp))
        )
    }
}

/**
 * Displays a preview of [TakeOfferComposable]
 */
@Preview
@Composable
fun PreviewTakeOfferComposable() {
    TakeOfferComposable(
        closeDialog = {},
        offerTruthSource = PreviewableOfferTruthSource(),
        id = Offer.sampleOffers[2].id
    )
}