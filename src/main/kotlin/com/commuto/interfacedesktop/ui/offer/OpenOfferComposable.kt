package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.TokenTransferApprovalState
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import com.commuto.interfacedesktop.ui.settlement.PreviewableSettlementMethodTruthSource
import com.commuto.interfacedesktop.ui.settlement.UISettlementMethodTruthSource
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * The screen for opening a new [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer).
 *
 * @param offerTruthSource The OffersViewModel that acts as a single source of truth for all offer-related data.
 * @param settlementMethodTruthSource An object implementing [UISettlementMethodTruthSource] that acts as a single
 * source of truth for all settlement-method-related data.
 * @param chainID The ID of the blockchain on which the new offer will be opened.
 * @param stablecoins A [StablecoinInformationRepository] for all supported stablecoins on the blockchain specified by
 * [chainID].
 */
@Composable
fun OpenOfferComposable(
    offerTruthSource: UIOfferTruthSource,
    settlementMethodTruthSource: UISettlementMethodTruthSource,
    chainID: BigInteger,
    stablecoins: StablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
) {

    /**
     * The direction of the new offer, or null if the user has not specified a direction.
     */
    val direction = remember { mutableStateOf<OfferDirection?>(null) }

    /**
     * The contract address of the stablecoin that the user is offering to swap, or null if the user has not specified
     * a stablecoin.
     */
    val selectedStablecoin = remember { mutableStateOf<String?>(null) }

    /**
     * The currency code of the specified stablecoin, or "Stablecoin" if the user has not specified a stablecoin.
     */
    val stablecoinCurrencyCode = stablecoins
        .getStablecoinInformation(chainID, selectedStablecoin.value ?: "")?.currencyCode ?: "Stablecoin"

    /**
     * The specified minimum stablecoin amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val minimumAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified minimum stablecoin amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val minimumAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The specified maximum stablecoin amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val maximumAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified maximum stablecoin amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val maximumAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The specified security deposit amount as a [String], or "0.00" if the user has not specified an amount.
     */
    val securityDepositAmountString = remember { mutableStateOf("0.00") }

    /**
     * The specified security deposit amount as a [BigDecimal], or [BigDecimal.ZERO] if the user has not specified an
     * amount.
     */
    val securityDepositAmount = remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * The [SettlementMethod]s that the user has selected.
     */
    val selectedSettlementMethods = remember { mutableStateListOf<SettlementMethod>() }

    /**
     * Indicates whether this is showing the dialog that allows the user to approve the token transfer in order to open
     * the offer.
     */
    val isShowingApproveTransferDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp)
            .padding(PaddingValues(bottom = 10.dp))
    ) {
        Text(
            text = "Open Offer",
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Direction:",
            style =  MaterialTheme.typography.h6,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val isBuyDirection = (direction.value == OfferDirection.BUY)
            val isSellDirection = (direction.value == OfferDirection.SELL)
            DirectionButtonComposable(
                isSelected = isBuyDirection,
                text = "Buy",
                onClick = {
                    direction.value = OfferDirection.BUY
                },
                modifier = Modifier.width(195.dp)
            )
            Spacer(
                modifier = Modifier.width(10.dp)
            )
            DirectionButtonComposable(
                isSelected = isSellDirection,
                text = "Sell",
                onClick = {
                    direction.value = OfferDirection.SELL
                },
                modifier = Modifier.width(195.dp)
            )
        }
        Text(
            text = "Stablecoin:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinListComposable(
            chainID = chainID,
            stablecoins = stablecoins,
            selectedStablecoin = selectedStablecoin
        )
        /*
        If the user sets a minimum amount greater than a maximum amount, set the maximum amount equal to the minimum
        amount
         */
        if (minimumAmount.value > maximumAmount.value) {
            maximumAmount.value = minimumAmount.value
            maximumAmountString.value = maximumAmount.value.toString()
        }
        Text(
            text = "Minimum $stablecoinCurrencyCode Amount:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinAmountFieldComposable(
            amountString = minimumAmountString,
            amount = minimumAmount
        )
        Text(
            text = "Maximum $stablecoinCurrencyCode Amount:",
            style =  MaterialTheme.typography.h6,
        )
        StablecoinAmountFieldComposable(
            amountString = maximumAmountString,
            amount = maximumAmount
        )
        if (maximumAmount.value > securityDepositAmount.value * BigDecimal.TEN) {
            securityDepositAmount.value = maximumAmount.value.divide(BigDecimal.TEN)
                .setScale(6, RoundingMode.UP)
            securityDepositAmountString.value = securityDepositAmount.value.toString()
        }
        DisclosureComposable(
            header = {
                Text(
                    text = "Security Deposit $stablecoinCurrencyCode Amount:",
                    style =  MaterialTheme.typography.h6,
                )
            },
            content = {
                Text(
                    text = "The Security Deposit Amount must be at least 10% of the Maximum Amount."
                )
            }
        )
        StablecoinAmountFieldComposable(
            amountString = securityDepositAmountString,
            amount = securityDepositAmount
        )
        DisclosureComposable(
            header = {
                Text(
                    text = "Service Fee Rate:",
                    style =  MaterialTheme.typography.h6,
                )
            },
            content = {
                Text(
                    text = "The percentage of the actual amount of stablecoin exchanged that you will be charged as " +
                            "a service fee."
                )
            }
        )
        if (offerTruthSource.isGettingServiceFeeRate.value) {
            Text(
                text = "Getting Service Fee Rate..."
            )
        } else {
            if (offerTruthSource.serviceFeeRate.value != null) {
                Text(
                    text = "${BigDecimal(offerTruthSource.serviceFeeRate.value)
                        .divide(BigDecimal.valueOf(100L)).setScale(2)} %",
                    style = MaterialTheme.typography.h4
                )
                ServiceFeeAmountComposable(
                    stablecoinInformation = stablecoins
                        .getStablecoinInformation(chainID, selectedStablecoin.value ?: ""),
                    minimumAmount = minimumAmount.value,
                    maximumAmount = maximumAmount.value,
                    serviceFeeRate = offerTruthSource.serviceFeeRate.value ?: BigInteger.valueOf(1)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Could not get service fee rate"
                    )
                    Button(
                        onClick = {
                            offerTruthSource.updateServiceFeeRate()
                        },
                        content = {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.h5
                            )
                        },
                        border = BorderStroke(3.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor =  Color.Transparent,
                            contentColor = Color.Black,
                        ),
                        elevation = null,
                    )
                }
            }
        }
        Text(
            text = "Settlement Methods:",
            style =  MaterialTheme.typography.h6,
        )
        SettlementMethodSelector(
            settlementMethodTruthSource = settlementMethodTruthSource,
            stablecoinCurrencyCode = stablecoinCurrencyCode,
            selectedSettlementMethods = selectedSettlementMethods
        )
        /*
        We don't want to display a description of the offer opening process state if an offer isn't being opened. We
        also don't want to do this if we encounter an exception; we should display the actual exception message instead.
         */
        if (offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState.VALIDATING ||
            offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState.SENDING_TRANSACTION
        ) {
            Text(
                text = "Approving $stablecoinCurrencyCode Transfer in Order to Open Offer",
                style =  MaterialTheme.typography.h6,
            )
        }
        if (offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState
                .AWAITING_TRANSACTION_CONFIRMATION) {
            Text(
                text = "Awaiting Confirmation of Transfer Approval. Please Find Your New Offer in the Offers List.",
                style =  MaterialTheme.typography.h6,
            )
        } else if (offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState.EXCEPTION) {
            Text(
                text = offerTruthSource.approvingTransferToOpenOfferException?.message ?: ("An unknown exception " +
                        "occurred"),
                style =  MaterialTheme.typography.h6,
                color = Color.Red
            )
        }
        val approveTransferButtonColor = when (offerTruthSource.approvingTransferToOpenOfferState.value)  {
            TokenTransferApprovalState.NONE, TokenTransferApprovalState.EXCEPTION -> Color.Black
            else -> Color.Gray
        }
        Button(
            onClick = {
                if (offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState.NONE ||
                    offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState.EXCEPTION) {
                    isShowingApproveTransferDialog.value = true
                }
            },
            content = {
                val approveTransferButtonLabel = when (offerTruthSource.approvingTransferToOpenOfferState.value)  {
                    TokenTransferApprovalState.NONE, TokenTransferApprovalState.EXCEPTION ->
                        "Approve Transfer to Open Offer"
                    TokenTransferApprovalState.AWAITING_TRANSACTION_CONFIRMATION ->
                        "Awaiting Confirmation of Transfer Approval"
                    else -> "Approving Transfer"
                }
                Text(
                    text = approveTransferButtonLabel,
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            border = BorderStroke(3.dp, approveTransferButtonColor),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =  Color.Transparent,
                contentColor = approveTransferButtonColor,
            ),
            elevation = null,
            modifier = Modifier.width(400.dp),
        )
    }
    if (isShowingApproveTransferDialog.value) {
        Dialog(
            onCloseRequest = {},
            state = DialogState(
                width = 500.dp,
                height = 600.dp,
            ),
            title = "Approve Token Transfer",
            undecorated = true,
            resizable = false,
            content = {
                Box(
                    modifier = Modifier
                        .width(600.dp)
                        .height(800.dp)
                ) {
                    TransactionGasDetailsComposable(
                        closeDialog = { isShowingApproveTransferDialog.value = false },
                        title = "Approve Token Transfer",
                        buttonLabel = "Approve Token Transfer",
                        buttonAction = { createdTransaction ->
                            offerTruthSource.approveTokenTransferToOpenOffer(
                                chainID = chainID,
                                stablecoin = selectedStablecoin.value,
                                stablecoinInformation = stablecoins.getStablecoinInformation(
                                    chainID = chainID,
                                    contractAddress = selectedStablecoin.value,
                                ),
                                minimumAmount = minimumAmount.value,
                                maximumAmount = maximumAmount.value,
                                securityDepositAmount = securityDepositAmount.value,
                                direction = direction.value,
                                settlementMethods = selectedSettlementMethods,
                                approveTokenTransferToOpenOfferTransaction = createdTransaction,
                            )
                        },
                        runOnAppearance = { approveTransferTransaction, transactionCreationException ->
                            if (offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState
                                    .NONE ||
                                offerTruthSource.approvingTransferToOpenOfferState.value == TokenTransferApprovalState
                                    .EXCEPTION
                            ) {
                                offerTruthSource.createApproveToOpenTransaction(
                                    stablecoin = selectedStablecoin.value,
                                    stablecoinInformation = stablecoins.getStablecoinInformation(
                                        chainID = chainID,
                                        contractAddress = selectedStablecoin.value,
                                    ),
                                    minimumAmount = minimumAmount.value,
                                    maximumAmount = maximumAmount.value,
                                    securityDepositAmount = securityDepositAmount.value,
                                    direction = direction.value,
                                    settlementMethods = selectedSettlementMethods,
                                    createdTransactionHandler = { createdTransaction ->
                                        approveTransferTransaction.value = createdTransaction
                                    },
                                    exceptionHandler = { exception ->
                                        transactionCreationException.value = exception
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

/**
 * Displays a button that represents an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)
 * direction (Buy or Sell).
 *
 * @param isSelected Indicates whether this button represents the currently selected offer direction.
 * @param text The text to display on this button.
 * @param onClick The action to perform when this button is clicked.
 * @param modifier The [Modifier] to apply to this button.
 */
@Composable
fun DirectionButtonComposable(isSelected: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        content = {
            Text(
                text = text,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
        },
        border = if (!isSelected) BorderStroke(1.dp, Color.Black) else null,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color.Green else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black,
        ),
        modifier = modifier,
        elevation = null,
    )
}

/**
 * Displays a vertical list of Stablecoin cards, one for each stablecoin with the specified chain ID in the specified
 * [StablecoinInformationRepository]. Only one stablecoin card can be selected at a time.
 *
 * @param chainID The blockchain ID of the stablecoins to be displayed.
 * @param stablecoins A [StablecoinInformationRepository] from which this will obtain a list of stablecoins.
 * @param selectedStablecoin The contract address of the currently selected stablecoin, or null if the user has not
 * selected a stablecoin.
 */
@Composable
fun StablecoinListComposable(
    chainID: BigInteger,
    stablecoins: StablecoinInformationRepository,
    selectedStablecoin: MutableState<String?>
) {

    /**
     * Key-value pairs of stablecoin addresses and [StablecoinInformation] objects obtained from [stablecoins]
     */
    val stablecoinInformationEntries = (stablecoins.stablecoinInformation[chainID] ?: mapOf()).entries

    Column {
        for(stablecoinInfoListEntry in stablecoinInformationEntries) {
            val color = if (selectedStablecoin.value == stablecoinInfoListEntry.key) Color.Green else Color.Black
            Button(
                onClick = {
                    selectedStablecoin.value = stablecoinInfoListEntry.key
                },
                content = {
                    Text(
                        text = stablecoinInfoListEntry.value.name,
                        fontWeight = FontWeight.Bold
                    )
                },
                border = BorderStroke(1.dp, color),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp)
                    .padding(vertical = 5.dp),
                elevation = null
            )
        }
    }
}

/**
 * Displays a preview of [OpenOfferComposable]
 */
@Preview
@Composable
fun PreviewOpenOfferComposable() {
    OpenOfferComposable(
        offerTruthSource = PreviewableOfferTruthSource(),
        settlementMethodTruthSource = PreviewableSettlementMethodTruthSource(),
        chainID = BigInteger.ONE
    )
}