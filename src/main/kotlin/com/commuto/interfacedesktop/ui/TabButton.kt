package com.commuto.interfacedesktop.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A button that lies in the tab sidebar [Composable], capable of setting the current tab.
 *
 * @param label The [String] that will be displayed as this button's label.
 * @param onClick The action that will be performed when this button is clicked.
 */
@Composable
fun TabButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor =  Color.Transparent,
            contentColor = Color.Black,
        ),
        elevation = null,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
    )
}

/**
 * Displays a preview of [TabButton].
 */
@Preview
@Composable
fun PreviewTabButton() {
    TabButton(
        label = "Preview",
        onClick = {},
    )
}