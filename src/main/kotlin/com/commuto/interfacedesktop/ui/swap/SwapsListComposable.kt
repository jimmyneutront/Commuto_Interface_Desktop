package com.commuto.interfacedesktop.ui.swap

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.swap.Swap

/**
 * Displays a list containing a [SwapCardComposable]-labeled [Button] for each swap in [Swap.sampleSwaps] that sets
 * [focusedSwap] equal to that swap and sets [focusedSwapComposable] to [FocusedSwapComposable.SwapComposable] when
 * pressed.
 *
 * @param modifier A [Modifier] to be applied to the outer column inside this [Composable].
 * @param focusedSwapComposable An enum representing the [Composable] currently being displayed on the trailing side of
 * this [SwapsListComposable].
 * @param focusedSwap The currently focused [Swap], the information of which will be displayed next to the list of
 * [Swap]s.
 */
@Composable
fun SwapsListComposable(
    modifier: Modifier,
    focusedSwapComposable: MutableState<FocusedSwapComposable>,
    focusedSwap: MutableState<Swap?>
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 10.dp)).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Swaps",
                style = MaterialTheme.typography.h2,
                fontWeight = FontWeight.Bold,
            )
        }
        Divider(
            modifier = Modifier.padding(horizontal = 10.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
        )
        LazyColumn {
            for (swap in Swap.sampleSwaps) {
                item {
                    Button(
                        onClick = {
                            focusedSwap.value = swap
                            focusedSwapComposable.value = FocusedSwapComposable.SwapComposable
                        },
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(PaddingValues(top = 5.dp))
                            .padding(horizontal = 5.dp),
                        contentPadding = PaddingValues(10.dp),
                        elevation = null,
                    ) {
                        SwapCardComposable(
                            swapDirection = swap.direction.string,
                            stablecoinCode = "STBL"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays a preview of [SwapsListComposable]
 */
@Preview
@Composable
fun PreviewSwapsListComposable() {
    SwapsListComposable(
        modifier = Modifier.widthIn(0.dp, 400.dp),
        focusedSwapComposable = mutableStateOf(FocusedSwapComposable.SwapComposable),
        focusedSwap = mutableStateOf(null)
    )
}