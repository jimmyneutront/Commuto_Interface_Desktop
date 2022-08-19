package com.commuto.interfacedesktop.ui.swap

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.swap.Swap

/**
 * Displays the [SwapsListComposable] for swaps and the focused [Swap], if any.
 *
 * @param swapTruthSource An object implementing [UISwapTruthSource] that acts as a single source of truth for all
 * swap-related data.
 */
@Composable
fun SwapsComposable(
    swapTruthSource: UISwapTruthSource
) {

    val focusedSwapComposable = remember { mutableStateOf(FocusedSwapComposable.SwapComposable) }

    val focusedSwap = remember { mutableStateOf<Swap?>(null) }

    Row {
        SwapsListComposable(
            modifier = Modifier.widthIn(100.dp, 300.dp),
            swapTruthSource = swapTruthSource,
            focusedSwapComposable = focusedSwapComposable,
            focusedSwap = focusedSwap
        )
        when (focusedSwapComposable.value) {
            FocusedSwapComposable.SwapComposable -> {
                if (focusedSwap.value != null) {
                    Text(focusedSwap.value?.id.toString())
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("No Swap Focused")
                    }
                }
            }
        }
    }
}

/**
 * Displays a preview of [SwapsComposable].
 */
@Preview
@Composable
fun PreviewSwapsComposable() {
    SwapsComposable(
        swapTruthSource = PreviewableSwapTruthSource()
    )
}