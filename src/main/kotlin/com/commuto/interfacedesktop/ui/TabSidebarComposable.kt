package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A horizontal list of buttons that control which [Composable] is shown on the trailing side of this [Composable].
 *
 * @param currentTab Indicates the currently displayed tab.
 */
@Composable
fun TabSidebarComposable(
    currentTab: MutableState<CurrentTab>
) {
    Column {
        Text(
            text = "Commuto",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        TabButton(
            label = "Offers",
            onClick = {
                currentTab.value = CurrentTab.OFFERS
            },
        )
        TabButton(
            label = "Swaps",
            onClick = {
                currentTab.value = CurrentTab.SWAPS
            },
        )
        TabButton(
            label = "Settlement Methods",
            onClick = {
                currentTab.value = CurrentTab.SETTLEMENT_METHODS
            }
        )
    }
}

/**
 * Displays a preview of [TabSidebarComposable].
 */
@Preview
@Composable
fun PreviewTabSidebarComposable() {
    TabSidebarComposable(
        currentTab = mutableStateOf(CurrentTab.OFFERS)
    )
}