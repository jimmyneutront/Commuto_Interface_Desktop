package com.commuto.interfacedesktop.ui.offer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
import java.lang.NumberFormatException
import java.math.BigDecimal

/**
 * Displays a [TextField] customized to accept positive stablecoin amounts with up to six decimal places of precision.
 * The user cannot type characters that are not numbers or decimal points, and cannot type negative numbers.
 *
 * @param amountString The current amount as a [String].
 * @param amount The current amount as a [BigDecimal].
 */
@Composable
fun StablecoinAmountFieldComposable(
    amountString: MutableState<String>,
    amount: MutableState<BigDecimal>
) {
    TextField(
        value = amountString.value,
        onValueChange = {
            if (it == "") {
                amount.value = BigDecimal.ZERO
                amountString.value = ""
            } else {
                try {
                    val newAmount = BigDecimal(it)
                    // Don't allow negative values, and limit to 6 decimal places
                    if (newAmount >= BigDecimal.ZERO && newAmount.scale() <= 6) {
                        amount.value = newAmount
                        amountString.value = it
                    }
                } catch (exception: NumberFormatException) {
                    /*
                    If the change prevents us from creating a BigDecimal minimum amount value, then we don't allow that
                    change
                     */
                }
            }
        },
        textStyle = MaterialTheme.typography.h5,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Displays a preview of [StablecoinAmountFieldComposable]
 */
@Preview
@Composable
fun PreviewStablecoinAmountFieldComposable() {
    StablecoinAmountFieldComposable(
        amountString = remember { mutableStateOf("0.00") },
        amount = remember { mutableStateOf(BigDecimal(0.00)) }
    )
}