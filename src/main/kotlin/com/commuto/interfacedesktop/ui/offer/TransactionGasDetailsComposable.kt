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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.transaction.type.Transaction1559

/**
 * Displays gas information about a [RawTransaction], or displays the localized description of the exception that
 * occurred while creating said [RawTransaction]. When this [Composable] appears, it executes [onRunAppearance‘, passing
 * said [RawTransaction] wrapped in a [MutableState], and a [MutableState] wrapping an optional [Exception], the
 * localized description of the value of which will be displayed to the user. [runOnAppearance] should be a closure that
 * creates a transaction and sets the result equal to value of said [MutableState]-wrapped [RawTransaction], or sets the
 * value of said [MutableState]-wrapped optional [Exception] to any exception that occurs during the transaction
 * creation process. If the value of said [MutableState]-wrapped [RawTransaction] is not `null`, the user can press the
 * main button in this Composable which will call [buttonAction], passing said [MutableState]-wrapped [RawTransaction].
 * This should only be presented inside a Dialog.
 */
@Composable
fun TransactionGasDetailsComposable(
    closeDialog: () -> Unit,
    title: String,
    buttonLabel: String,
    buttonAction: (RawTransaction) -> Unit,
    runOnAppearance: (MutableState<RawTransaction?>, MutableState<Exception?>) -> Unit
) {

    /**
     * The created [RawTransaction] about which this displays details (and which will be passed to [buttonAction]) or
     * `null` if no such transaction is available.
     */
    val transaction = remember { mutableStateOf<RawTransaction?>(null) }

    /**
     * The [Exception] that occurred in [runOnAppearance], or `null` if no such error has occurred.
     */
    val transactionCreationException = remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(null) {
        runOnAppearance(transaction, transactionCreationException)
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
                text = title,
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
        val rawTransaction = transaction.value
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
                    // Close the sheet in which this view is presented as soon as the user presses this button.
                    buttonAction(rawTransaction)
                    closeDialog()
                },
                content = {
                    Text(
                        text = buttonLabel,
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
 * Displays a preview of [TransactionGasDetailsComposable]
 */
@Preview
@Composable
fun PreviewTransactionGasDetailsComposable() {
    TransactionGasDetailsComposable(
        closeDialog = {},
        title = "Cancel Offer",
        buttonLabel = "Cancel Offer",
        buttonAction = {},
        runOnAppearance = {_, _ ->},
    )
}
