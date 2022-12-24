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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.offer.CancelingOfferState
import com.commuto.interfacedesktop.offer.Offer
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.transaction.type.Transaction1559

/**
 * Allows the user to cancel an [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) that they
 * have made, and displays the necessary gas and gas cost before actually cancelling the Offer. This should only be
 * presented inside a sheet.
 *
 * @param offer The [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer) that this
 * [CancelOfferComposable] allows the user to cancel.
 * @param closeDialog The lambda that can close the dialog in which this [Composable] is displayed.
 * @param offerTruthSource The [OffersViewModel] that acts as a single source of truth for all offer-related data.
 */
@Composable
fun CancelOfferComposable(
    offer: Offer,
    closeDialog: () -> Unit,
    offerTruthSource: UIOfferTruthSource,
) {

    /**
     * The created [RawTransaction] that will cancel the offer, or `null` if no such transaction is available.
     */
    val cancelOfferTransaction = remember { mutableStateOf<RawTransaction?>(null) }

    /**
     * The [Exception] that has occurred while creating the transaction for offer cancellation, or `null` if no such
     * exception has occurred.
     */
    val transactionCreationException = remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(true) {
        if (offer.cancelingOfferState.value == CancelingOfferState.NONE ||
            offer.cancelingOfferState.value == CancelingOfferState.EXCEPTION) {
            offerTruthSource.createCancelOfferTransaction(
                offer = offer,
                createdTransactionHandler = { createdTransaction ->
                    cancelOfferTransaction.value = createdTransaction
                },
                exceptionHandler = { exception ->
                    transactionCreationException.value = exception
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cancel Offer",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = {
                    closeDialog()
                },
                content = {
                    Text(
                        text = "Close",
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
        val rawTransaction = cancelOfferTransaction.value
        if (rawTransaction != null) {
            val eip1559Transaction = (rawTransaction.transaction as Transaction1559)
            Text(
                text = "Estimated Gas:",
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = eip1559Transaction.gasLimit.toString(),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Max Fee Per Gas (Wei):",
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = eip1559Transaction.maxFeePerGas.toString(),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Estimated Total Cost (Wei):",
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = (eip1559Transaction.gasLimit * eip1559Transaction.maxFeePerGas).toString(),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            if (transactionCreationException.value != null) {
                Text(
                    text = transactionCreationException.value?.message ?: "An unknown exception occurred",
                    color = Color.Red
                )
            }
            Button(
                onClick = {
                    // Close the sheet in which this view is presented as soon as the user begins the canceling process.
                    offerTruthSource.cancelOffer(
                        offer = offer,
                        offerCancellationTransaction = cancelOfferTransaction.value
                    )
                    closeDialog()
                },
                content = {
                    Text(
                        text = "Cancel Offer",
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
        } else if (transactionCreationException.value != null) {
            Text(
                text = transactionCreationException.value?.message ?: "An unknown exception occurred",
                color = Color.Red
            )
        } else {
            Text(
                text = "Creating Transaction..."
            )
        }
    }

}

/**
 * Displays a preview of [CancelOfferComposable]
 */
@Preview
@Composable
fun PreviewCancelOfferComposable() {
    CancelOfferComposable(
        offer = Offer.sampleOffers[2],
        closeDialog = {},
        offerTruthSource = PreviewableOfferTruthSource()
    )
}