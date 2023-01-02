package com.commuto.interfacedesktop.ui.swap

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.swap.*
import com.commuto.interfacedesktop.ui.StablecoinInformation
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import com.commuto.interfacedesktop.ui.offer.TransactionGasDetailsComposable
import com.commuto.interfacedesktop.ui.settlement.SettlementMethodPrivateDetailComposable
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 * Displays information about a specific [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
 *
 * @param swapTruthSource The [UISwapTruthSource] that acts as a single source of truth for all swap-related data.
 * @param id The ID of the swap about which this [SwapComposable] is displaying information.
 * @param stablecoinInfoRepo The [StablecoinInformationRepository] that this [Composable] uses to get stablecoin name
 * and currency code information. Defaults to [StablecoinInformationRepository.hardhatStablecoinInfoRepo] if no
 * other value is passed.
 */
@Composable
fun SwapComposable(
    swapTruthSource: UISwapTruthSource,
    id: UUID?,
    stablecoinInfoRepo: StablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo,
) {

    /**
     * The [Swap] about which this displays information.
     */
    val swap = swapTruthSource.swaps[id]

    if (id == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Swap has an invalid ID.",
            )
        }
    } else if (swap == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This Swap is not available.",
            )
        }
    } else {
        val stablecoinInformation = stablecoinInfoRepo.getStablecoinInformation(swap.chainID, swap.stablecoin)
        val counterpartyDirection: String = remember {
            when (swap.role) {
                SwapRole.MAKER_AND_BUYER, SwapRole.TAKER_AND_BUYER -> "Seller"
                SwapRole.MAKER_AND_SELLER, SwapRole.TAKER_AND_SELLER -> "Buyer"
            }
        }

        /**
         * The settlement method of `swap`, but with the private data belonging to the user of this interface.
         */
        val settlementMethodOfUser = SettlementMethod(
            currency = swap.settlementMethod.currency,
            method = swap.settlementMethod.method,
            price = swap.settlementMethod.price,
        )

        /**
         * The settlement method of `swap`, but with the private data of the counterparty, if any.
         */
        val settlementMethodOfCounterparty = SettlementMethod(
            currency = swap.settlementMethod.currency,
            method = swap.settlementMethod.method,
            price = swap.settlementMethod.price,
        )

        when (swap.role) {
            SwapRole.MAKER_AND_BUYER, SwapRole.MAKER_AND_SELLER -> {
                settlementMethodOfUser.privateData = swap.makerPrivateSettlementMethodData
                settlementMethodOfCounterparty.privateData = swap.takerPrivateSettlementMethodData
            }
            SwapRole.TAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> {
                settlementMethodOfUser.privateData = swap.takerPrivateSettlementMethodData
                settlementMethodOfCounterparty.privateData = swap.makerPrivateSettlementMethodData
            }
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(9.dp)
        ) {
            Text(
                text = "Swap",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Role:",
                style =  MaterialTheme.typography.h6,
            )
            Text(
                text = createRoleDescription(
                    swapRole = swap.role,
                    stablecoinInformation = stablecoinInformation,
                ),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )
            SwapAmountComposable(
                stablecoinInformation = stablecoinInformation,
                amount = swap.takenSwapAmount,
                securityDeposit = swap.securityDepositAmount,
                serviceFee = swap.serviceFeeAmount,
            )
            SwapSettlementMethodComposable(
                stablecoinInformation = stablecoinInformation,
                settlementMethod = swap.settlementMethod,
                swapAmount = swap.takenSwapAmount,
            )
            Text(
                text = "$counterpartyDirection's Details:",
                style =  MaterialTheme.typography.h6,
            )
            if (settlementMethodOfCounterparty.privateData != null) {
                SettlementMethodPrivateDetailComposable(settlementMethod = settlementMethodOfCounterparty)
            } else {
                Text(
                    text = "Not yet received."
                )
            }
            Text(
                text = "Your Details:",
                style =  MaterialTheme.typography.h6,
            )
            SettlementMethodPrivateDetailComposable(settlementMethod = settlementMethodOfUser)
            Text(
                text = "State:",
                style =  MaterialTheme.typography.h6,
            )
            SwapStateComposable(
                swapState = swap.state,
                userRole = swap.role,
                settlementMethodCurrency = swap.settlementMethod.currency,
            )
            ActionButton(
                swap = swap,
                swapTruthSource = swapTruthSource,
            )
            Button(
                onClick = {},
                content = {
                    Text(
                        text = "Chat",
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(
                modifier = Modifier.height(9.dp)
            )
            Button(
                onClick = {},
                content = {
                    Text(
                        text = "Raise Dispute",
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Creates a role description string (such as "Buying USDC as maker" or "Selling DAI as taker") given the user's
 * [SwapRole] and an optional [StablecoinInformation].
 *
 * @param swapRole The user's [SwapRole] for this swap.
 * @param stablecoinInformation An optional [StablecoinInformation] for this swap's stablecoin. If this is `null`, this
 * uses the symbol "Unknown Stablecoin".
 *
 * @return A role description.
 */
fun createRoleDescription(swapRole: SwapRole, stablecoinInformation: StablecoinInformation?): String {
    val direction = when (swapRole) {
        SwapRole.MAKER_AND_BUYER, SwapRole.TAKER_AND_BUYER -> "Buying"
        SwapRole.MAKER_AND_SELLER, SwapRole.TAKER_AND_SELLER -> "Selling"
    }
    val currencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val role = when (swapRole) {
        SwapRole.MAKER_AND_BUYER, SwapRole.MAKER_AND_SELLER -> "maker"
        SwapRole.TAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> "taker"
    }
    return "$direction $currencyCode as $role"
}

/**
 * A [Composable] that displays the amount, service fee, and security deposit for a
 * [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap).
 *
 * @param stablecoinInformation An optional [StablecoinInformation] for the swap's stablecoin.
 * @param amount The [Swap]'s [Swap.takenSwapAmount] property.
 * @param securityDeposit The [Swap]'s [Swap.securityDepositAmount].
 * @param serviceFee The [Swap]'s [Swap.serviceFeeAmount]`.
 */
@Composable
fun SwapAmountComposable(
    stablecoinInformation: StablecoinInformation?,
    amount: BigInteger,
    securityDeposit: BigInteger,
    serviceFee: BigInteger,
) {
    val currencyCode = stablecoinInformation?.currencyCode ?: "Unknown Stablecoin"
    val amountHeader = if (stablecoinInformation != null) {
        "Amount:"
    } else {
        "Amount (in token base units):"
    }
    val amountString = if (stablecoinInformation != null) amount.divide(BigInteger.TEN.pow(stablecoinInformation
        .decimal)).toString() else amount.toString()
    val securityDepositString = if (stablecoinInformation != null) securityDeposit.divide(BigInteger.TEN
        .pow(stablecoinInformation.decimal)).toString() else amount.toString()
    val serviceFeeString = if (stablecoinInformation != null) serviceFee.divide(BigInteger.TEN.pow(stablecoinInformation
        .decimal)).toString() else amount.toString()

    Text(
        text = amountHeader,
        style = MaterialTheme.typography.h6
    )
    Text(
        text = "$amountString $currencyCode",
        style =  MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Service Fee Amount:",
        style = MaterialTheme.typography.h6
    )
    Text(
        text = "$serviceFeeString $currencyCode",
        style =  MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Security Deposit Amount:",
        style = MaterialTheme.typography.h6
    )
    Text(
        text = "$securityDepositString $currencyCode",
        style =  MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Displays a card containing information about the swap's settlement method.
 *
 * @param stablecoinInformation An optional [StablecoinInformation] for the swap's stablecoin. If this is `null`,
 * the price description will be "Unable to determine price" and the total string will be "Unable to calculate total".
 * @param settlementMethod The swap's [SettlementMethod].
 * @param swapAmount The swap's taken swap amount.
 */
@Composable
fun SwapSettlementMethodComposable(
    stablecoinInformation: StablecoinInformation?,
    settlementMethod: SettlementMethod,
    swapAmount: BigInteger) {

    /**
     * A human readable string describing the currency and transfer method, such as "EUR via SEPA"
     */
    val currencyDescription = "${settlementMethod.currency} via ${settlementMethod.method}"

    /**
     * A human readable string describing the price for the settlement method, such as "Price: 0.94 EUR/DAI" or "Price:
     * 1.00 USD/USDC".
     */
    var priceDescription = "Unable to determine price"

    /**
     * A human readable string describing the total traditional currency cost of this swap, such as "15000 EUR" or
     * "1000 USD".
     */
    var totalString = "Unable to calculate total"

    if (stablecoinInformation != null) {
        priceDescription = "Price: ${settlementMethod.price} ${settlementMethod.currency}/" +
                stablecoinInformation.currencyCode
        val amountBigDecimal = swapAmount.toBigDecimal() / BigDecimal.TEN.pow(stablecoinInformation.decimal)
        val priceBigDecimal = try {
            BigDecimal(settlementMethod.price)
        } catch (e: Exception) {
            null
        }
        if (priceBigDecimal != null) {
            val total = amountBigDecimal * priceBigDecimal
            totalString = "$total ${settlementMethod.currency}"
        }
    }

    Column(
        modifier = Modifier
            .border(width = 1.dp, color = Color.Black, CutCornerShape(0.dp))
            .padding(9.dp)
    ) {
        Text(
            text = currencyDescription,
            modifier = Modifier.padding(3.dp)
        )
        Text(
            text = priceDescription,
            modifier = Modifier.padding(3.dp)
        )
        Text(
            text = totalString,
            modifier = Modifier.padding(3.dp)
        )
    }

}

/**
 * Displays a human readable string describing the swap's current state.
 *
 * @param swapState The [Swap.state] property of the [Swap] that this view represents.
 * @param userRole The [Swap.role] property of the [Swap] that this view represents.
 * @param settlementMethodCurrency The currency code of this [Swap]'s settlement method.
 */
@Composable
fun SwapStateComposable(swapState: MutableState<SwapState>, userRole: SwapRole, settlementMethodCurrency: String) {
    /**
     * A name by which we can refer to the stablecoin buyer. If the user is the buyer, this is "you". If the user is the
     * seller, this is "buyer".
     */
    val buyerName = when (userRole) {
        // The user is the buyer, so the buyer's name is "you"
        SwapRole.MAKER_AND_BUYER, SwapRole.TAKER_AND_BUYER -> "you"
        // The user is the seller, so the buyer's name is "buyer"
        SwapRole.MAKER_AND_SELLER, SwapRole.TAKER_AND_SELLER -> "buyer"
    }

    /**
     * A name by which we can refer to the stablecoin seller. If the user is the buyer, this is "seller". If the user is
     * the seller, this is "you".
     */
    val sellerName = when (userRole) {
        // The user is the buyer, so the seller's name is "seller"
        SwapRole.MAKER_AND_BUYER, SwapRole.TAKER_AND_BUYER -> "you"
        // The user is the seller, so the seller's name is "you"
        SwapRole.MAKER_AND_SELLER, SwapRole.TAKER_AND_SELLER -> "buyer"
    }

    /**
     * A string describing [swapState] based on the value of [userRole].
     */
    val stateDescription = when (swapState.value) {
        SwapState.TAKING -> "Taking Offer..."
        SwapState.TAKE_OFFER_TRANSACTION_BROADCAST -> "Awaiting confirmation that offer is taken"
        SwapState.AWAITING_TAKER_INFORMATION -> {
            when (userRole) {
                // We are the maker, so we are waiting to receive settlement method information from the taker
                SwapRole.MAKER_AND_BUYER, SwapRole.MAKER_AND_SELLER -> "Waiting to receive details from taker"
                // We are the taker, so we are sending settlement method information to the maker
                SwapRole.TAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> "Sending details to maker"
            }
        }
        SwapState.AWAITING_MAKER_INFORMATION -> {
            when (userRole) {
                // We are the maker, so we are sending settlement method information to the taker
                SwapRole.MAKER_AND_BUYER, SwapRole.MAKER_AND_SELLER -> "Sending details to taker"
                // We are the taker, so we are waiting to receive settlement method information from the maker
                SwapRole.TAKER_AND_BUYER, SwapRole.TAKER_AND_SELLER -> "Waiting to receive details from maker"
            }
        }
        SwapState.AWAITING_FILLING -> {
            /*
            The offer should only be in this state if it is a maker-as-seller offer, therefore if we are not the maker
            and seller, we are waiting for the maker/seller to fill the swap
             */
            when (userRole) {
                SwapRole.MAKER_AND_SELLER -> "Waiting for you to fill swap"
                else -> "Waiting for maker to fill swap"
            }
        }
        SwapState.FILL_SWAP_TRANSACTION_BROADCAST -> "Awaiting confirmation that swap is filled"
        SwapState.AWAITING_PAYMENT_SENT -> {
            "Waiting for $buyerName to send $settlementMethodCurrency"
        }
        SwapState.REPORT_PAYMENT_SENT_TRANSACTION_BROADCAST -> "Awaiting confirmation of reporting that payment is sent"
        SwapState.AWAITING_PAYMENT_RECEIVED -> {
            "Waiting for $sellerName to receive $settlementMethodCurrency"
        }
        SwapState.REPORT_PAYMENT_RECEIVED_TRANSACTION_BROADCAST -> "Awaiting confirmation of reporting that payment " +
                "is received"
        SwapState.AWAITING_CLOSING -> "Waiting for you to close swap"
        SwapState.CLOSE_SWAP_TRANSACTION_BROADCAST -> "Awaiting confirmation that swap is closed"
        SwapState.CLOSED -> "Swap Closed"
    }

    Text(
        text = stateDescription,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )

}

/**
 * Displays a button allowing the user to execute the current action that must be completed in order to continue the
 * swap process, or displays nothing if the user cannot execute said action.
 *
 * For example, if the current state of the swap is [SwapState.AWAITING_PAYMENT_SENT] and the user is the buyer, then
 * the user must confirm that they have sent payment in order for the swap process to continue. Therefore this will
 * display a button with the label "Confirm Payment is Sent" which allows the user to do so. However, if the user is the
 * seller, they cannot confirm payment is sent, since that is the buyer's responsibility. In that case, this would
 * display nothing.
 *
 * @param swap The [Swap] for which this [ActionButton] displays a button.
 * @param swapTruthSource The [UISwapTruthSource] that acts as a single source of truth for all swap-related data.
 */
@Composable
fun ActionButton(swap: Swap, swapTruthSource: UISwapTruthSource) {
    if (swap.state.value == SwapState.AWAITING_FILLING && swap.role == SwapRole.MAKER_AND_SELLER) {
        // If the swap state is awaitingFilling and we are the maker and seller, then we display the "Fill Swap" button
        if (swap.fillingSwapState.value != FillingSwapState.NONE &&
            swap.fillingSwapState.value != FillingSwapState.EXCEPTION) {
            Text(
                text = swap.fillingSwapState.value.description,
                style =  MaterialTheme.typography.h6,
            )
        }
        if (swap.fillingSwapState.value == FillingSwapState.EXCEPTION) {
            Text(
                text = swap.fillingSwapException?.message ?: "An unknown exception occurred",
                style =  MaterialTheme.typography.h6,
                color = Color.Red
            )
        }
        BlankActionButton(
            action = {
                if (swap.fillingSwapState.value == FillingSwapState.NONE ||
                    swap.fillingSwapState.value == FillingSwapState.EXCEPTION) {
                    swapTruthSource.fillSwap(
                        swap = swap
                    )
                }
            },
            labelText = when (swap.fillingSwapState.value) {
                FillingSwapState.NONE, FillingSwapState.EXCEPTION -> "Fill Swap"
                FillingSwapState.COMPLETED -> "Swap Filled"
                else -> "Filling Swap"
            }
        )
    } else if (swap.state.value == SwapState.AWAITING_PAYMENT_SENT && (swap.role == SwapRole.MAKER_AND_SELLER ||
                swap.role == SwapRole.TAKER_AND_SELLER)) {
        /*
        If the swap state is awaitingPaymentSent and we are the buyer, then we display the "Confirm Payment is Sent"
        button
         */

        /**
         * Indicates whether we are showing the sheet that allows the user to report that they have sent payment, if
         * they are the buyer.
         */
        val isShowingReportPaymentSentDialog = remember { mutableStateOf(false) }

        if (swap.reportingPaymentSentState.value != ReportingPaymentSentState.NONE &&
            swap.reportingPaymentSentState.value != ReportingPaymentSentState.EXCEPTION) {
            Text(
                text = swap.reportingPaymentSentState.value.description,
                style =  MaterialTheme.typography.h6,
            )
        } else if (swap.reportingPaymentSentState.value == ReportingPaymentSentState.EXCEPTION) {
            Text(
                text = swap.reportingPaymentSentException?.message ?: "An unknown exception occurred",
                style =  MaterialTheme.typography.h6,
                color = Color.Red
            )
        }
        BlankActionButton(
            action = {
                if (swap.reportingPaymentSentState.value == ReportingPaymentSentState.NONE ||
                    swap.reportingPaymentSentState.value == ReportingPaymentSentState.EXCEPTION) {
                    isShowingReportPaymentSentDialog.value = true
                }
            },
            labelText = when (swap.reportingPaymentSentState.value) {
                ReportingPaymentSentState.NONE, ReportingPaymentSentState.EXCEPTION -> "Report that Payment Is Sent"
                ReportingPaymentSentState.COMPLETED -> "Reported that Payment Is Sent"
                else -> "Reporting that Payment Is Sent"
            }
        )
        if (isShowingReportPaymentSentDialog.value) {
            Dialog(
                onCloseRequest = {},
                state = DialogState(
                    width = 500.dp,
                    height = 600.dp,
                ),
                title = "Report Payment Sent",
                undecorated = true,
                resizable = false,
                content = {
                    Box(
                        modifier = Modifier
                            .width(600.dp)
                            .height(800.dp)
                    ) {
                        TransactionGasDetailsComposable(
                            closeDialog = { isShowingReportPaymentSentDialog.value = false },
                            title = "Report Payment Sent",
                            buttonLabel = "Report Payment Sent",
                            buttonAction = { createdTransaction ->
                                swapTruthSource.reportPaymentSent(
                                    swap = swap,
                                    reportPaymentSentTransaction = createdTransaction,
                                )
                            },
                            runOnAppearance = { reportPaymentSentTransaction, transactionCreationException ->
                                if (swap.reportingPaymentSentState.value == ReportingPaymentSentState.NONE ||
                                    swap.reportingPaymentSentState.value == ReportingPaymentSentState.EXCEPTION) {
                                    swapTruthSource.createReportPaymentSentTransaction(
                                        swap = swap,
                                        createdTransactionHandler = { createdTransaction ->
                                            reportPaymentSentTransaction.value = createdTransaction
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
    } else if (swap.state.value == SwapState.AWAITING_PAYMENT_RECEIVED && (swap.role == SwapRole.MAKER_AND_BUYER ||
                swap.role == SwapRole.TAKER_AND_BUYER)) {
        /*
        If the swap state is awaitingPaymentReceived and we are the seller, then we display the "Confirm Payment is
        Received" button
         */
        if (swap.reportingPaymentReceivedState.value != ReportingPaymentReceivedState.NONE &&
            swap.reportingPaymentReceivedState.value != ReportingPaymentReceivedState.EXCEPTION) {
            Text(
                text = swap.reportingPaymentReceivedState.value.description,
                style =  MaterialTheme.typography.h6,
            )
        } else if (swap.reportingPaymentReceivedState.value == ReportingPaymentReceivedState.EXCEPTION) {
            Text(
                text = swap.reportingPaymentReceivedException?.message ?: "An unknown exception occurred",
                style =  MaterialTheme.typography.h6,
                color = Color.Red
            )
        }
        BlankActionButton(
            action = {
                if (swap.reportingPaymentReceivedState.value == ReportingPaymentReceivedState.NONE ||
                    swap.reportingPaymentReceivedState.value == ReportingPaymentReceivedState.EXCEPTION) {
                    swapTruthSource.reportPaymentReceived(
                        swap = swap
                    )
                }
            },
            labelText = when (swap.reportingPaymentReceivedState.value) {
                ReportingPaymentReceivedState.NONE, ReportingPaymentReceivedState.EXCEPTION -> "Report that Payment " +
                        "Is Received"
                ReportingPaymentReceivedState.COMPLETED -> "Reported that Payment Is Received"
                else -> "Reporting that Payment Is Received"
            }
        )
    } else if (swap.state.value == SwapState.AWAITING_CLOSING) {
        // We can now close the swap, so we display the "Close Swap" button
        if (swap.closingSwapState.value != ClosingSwapState.NONE &&
            swap.closingSwapState.value != ClosingSwapState.EXCEPTION) {
            Text(
                text = swap.closingSwapState.value.description,
                style =  MaterialTheme.typography.h6,
            )
        } else if (swap.closingSwapState.value == ClosingSwapState.EXCEPTION) {
            Text(
                text = swap.closingSwapException?.message ?: "An unknown exception occurred",
                style =  MaterialTheme.typography.h6,
                color = Color.Red
            )
        }
        BlankActionButton(
            action = {
                if (swap.closingSwapState.value == ClosingSwapState.NONE ||
                    swap.closingSwapState.value == ClosingSwapState.EXCEPTION) {
                    swapTruthSource.closeSwap(
                        swap = swap
                    )
                }
            },
            labelText = when (swap.closingSwapState.value) {
                ClosingSwapState.NONE, ClosingSwapState.EXCEPTION -> "Close Swap"
                ClosingSwapState.COMPLETED -> "Swap Closed"
                else -> "Closing Swap"
            }
        )
    }
}

/**
 * Displays a button with the specified [labelText] with a black rounded rectangle border that performs [action] when
 * clicked.
 *
 * @param action The lambda to execute when the button is clicked.
 * @param labelText The [String] that will be displayed as the label of this button.
 */
@Composable
fun BlankActionButton(action: () -> Unit, labelText: String) {
    Button(
        onClick = action,
        content = {
            Text(
                text = labelText,
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
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Displays a preview of [SwapComposable] with a sample [Swap]
 */
@Preview
@Composable
fun PreviewSwapComposable() {
    SwapComposable(
        swapTruthSource = PreviewableSwapTruthSource(),
        id = Swap.sampleSwaps[0].id,
    )
}