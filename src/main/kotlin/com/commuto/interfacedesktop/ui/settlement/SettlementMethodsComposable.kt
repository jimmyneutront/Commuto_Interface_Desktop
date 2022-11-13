package com.commuto.interfacedesktop.ui.settlement

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Displays the list of the user's settlement methods as [SettlementMethodCardComposable]s in a [LazyColumn].
 */
@Composable
fun SettlementMethodsComposable() {

    val focusedSettlementMethodComposable = remember {
        mutableStateOf(FocusedSettlementMethodComposable.SettlementMethodComposable)
    }

    val focusedSettlementMethod = remember { mutableStateOf<SettlementMethod?>(null) }

    Row {
        Column(
            modifier = Modifier.widthIn(100.dp, 300.dp),
        ) {
            Row(
                modifier = Modifier.padding(PaddingValues(start = 10.dp)).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settlement Methods",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = {},
                    content = {
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = Color.Black,
                    ),
                    border = BorderStroke(1.dp, Color.Black),
                    elevation = null
                )
            }
            Divider(
                modifier = Modifier.padding(horizontal = 10.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
            )
            LazyColumn {
                for (entry in SettlementMethod.sampleSettlementMethodsEmptyPrices) {
                    item {
                        Button(
                            onClick = {
                                focusedSettlementMethod.value = entry
                                focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                                    .SettlementMethodComposable
                            },
                            border = BorderStroke(1.dp, Color.Black),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .padding(PaddingValues(top = 5.dp, start = 5.dp)),
                            contentPadding = PaddingValues(10.dp),
                            elevation = null,
                        ) {
                            SettlementMethodCardComposable(
                                settlementMethod = entry
                            )
                        }
                    }
                }
            }
        }
        when (focusedSettlementMethodComposable.value) {
            FocusedSettlementMethodComposable.SettlementMethodComposable -> {
                if (focusedSettlementMethod.value != null) {
                    Text(focusedSettlementMethod.value?.currency ?: "unkwn")
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "No Settlement Method Selected"
                        )
                    }
                }
            }
        }
    }
}

/**
 * A card displaying basic information about a settlement method belonging to the user, to be shown in the list of the
 * user's settlement methods.
 * @param settlementMethod The [SettlementMethod] about which information will be displayed.
 */
@Composable
fun SettlementMethodCardComposable(settlementMethod: SettlementMethod) {
    val detailString = createDetailString(settlementMethod = settlementMethod)
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = settlementMethod.method,
            fontWeight = FontWeight.Bold
        )
        Text(settlementMethod.currency)
        Text(
            text = detailString,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

fun createDetailString(settlementMethod: SettlementMethod): String {
    val privateDataString = settlementMethod.privateData
    val detailString = privateDataString ?: "Unable to parse data"
    if (privateDataString != null) {
        try {
            val privateSEPAData = Json.decodeFromString<PrivateSEPAData>(privateDataString)
            return "IBAN: ${privateSEPAData.iban}"
        } catch (_: Exception) {}
        try {
            val privateSWIFTData = Json.decodeFromString<PrivateSWIFTData>(settlementMethod.privateData ?: "")
            return "Account: ${privateSWIFTData.accountNumber}"
        } catch (_: Exception) {}
    }
    return detailString
}

@Preview
@Composable
fun PreviewSettlementMethodsComposable() {
    SettlementMethodsComposable()
}