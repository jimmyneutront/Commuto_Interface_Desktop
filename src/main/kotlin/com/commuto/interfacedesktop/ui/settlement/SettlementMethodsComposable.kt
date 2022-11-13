package com.commuto.interfacedesktop.ui.settlement

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                    SettlementMethodDetailComposable(
                        settlementMethod = focusedSettlementMethod.value
                    )
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

    val privateData = remember { mutableStateOf<PrivateData?>(null) }
    val finishedParsingData = remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        createDetailString(
            settlementMethod = settlementMethod,
            privateData = privateData,
            finishedParsingData = finishedParsingData
        )
    }
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = settlementMethod.method,
            fontWeight = FontWeight.Bold
        )
        Text(settlementMethod.currency)
        val privateDataValue = privateData.value
        if (settlementMethod.privateData != null) {
            if (privateDataValue is PrivateSEPAData) {
                Text(
                    text = "IBAN: ${privateDataValue.iban}",
                    style = MaterialTheme.typography.subtitle1
                )
            } else if (privateDataValue is PrivateSWIFTData){
                Text(
                    text = "Account: ${privateDataValue.accountNumber}",
                    style = MaterialTheme.typography.subtitle1
                )
            } else if (finishedParsingData.value) {
                Text(
                    text = settlementMethod.privateData ?: "Settlement method has no private data",
                    style = MaterialTheme.typography.subtitle1
                )
            }
        } else {
            Text(
                text = "Unable to parse data",
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

/**
 * Displays all information, including private information, about a given [SettlementMethod].
 */
@Composable
fun SettlementMethodDetailComposable(
    settlementMethod: SettlementMethod?
) {

    val privateData = remember { mutableStateOf<PrivateData?>(null) }
    val finishedParsingData = remember { mutableStateOf(false) }

    if (settlementMethod != null) {
        LaunchedEffect(settlementMethod) {
            createDetailString(
                settlementMethod = settlementMethod,
                privateData = privateData,
                finishedParsingData = finishedParsingData
            )
        }
        val navigationTitle = when (settlementMethod.method) {
            "SEPA" -> {
                "SEPA Transfer"
            }
            "SWIFT" -> {
                "SWIFT Transfer"
            }
            else -> {
                settlementMethod.method
            }
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(9.dp)
        ) {
            Text(
                text = navigationTitle,
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = settlementMethod.currency,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            if (privateData.value != null) {
                when (privateData.value) {
                    is PrivateSEPAData -> {
                        SEPADetailComposable(privateData.value as PrivateSEPAData)
                    }
                    is PrivateSWIFTData -> {
                        SWIFTDetailComposable(privateData.value as PrivateSWIFTData)
                    }
                    else -> {
                        Text(
                            text = "Unknown Settlement Method Type"
                        )
                    }
                }
            } else if (finishedParsingData.value) {
                Text(
                    text = "Unable to parse data",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "This settlement method is not available.",
            )
        }
    }
}

/**
 * Displays private SEPA account information.
 */
@Composable
fun SEPADetailComposable(privateData: PrivateSEPAData) {
    Text(
        text = "Account Holder:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.accountHolder,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "BIC:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.bic,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "IBAN:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.iban,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Address:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.address,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Displays private SWIFT account information.
 */
@Composable
fun SWIFTDetailComposable(privateData: PrivateSWIFTData) {
    Text(
        text = "Account Holder:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.accountHolder,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "BIC:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.bic,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Account Number:",
        style = MaterialTheme.typography.h5,
    )
    Text(
        text = privateData.accountNumber,
        style = MaterialTheme.typography.h4,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Attempts to create a private data structure by deserializing the private data of [settlementMethod], and then on the
 * main coroutine dispatcher, sets the value of [privateData] equal to the result and sets the value of
 * [finishedParsingData] to true.
 */
suspend fun createDetailString(
    settlementMethod: SettlementMethod,
    privateData: MutableState<PrivateData?>,
    finishedParsingData: MutableState<Boolean>,
) {
    withContext(Dispatchers.IO) {
        val privateDataString = settlementMethod.privateData
        if (privateDataString != null) {
            try {
                val privateSEPAData = Json.decodeFromString<PrivateSEPAData>(privateDataString)
                withContext(Dispatchers.Main) {
                    privateData.value = privateSEPAData
                    finishedParsingData.value = true
                }
                return@withContext
            } catch (_: Exception) {}
            try {
                val privateSWIFTData = Json.decodeFromString<PrivateSWIFTData>(privateDataString)
                withContext(Dispatchers.Main) {
                    privateData.value = privateSWIFTData
                    finishedParsingData.value = true
                }
                return@withContext
            } catch (_: Exception) {}
        }
        finishedParsingData.value = true
        return@withContext
    }
}

@Preview
@Composable
fun PreviewSettlementMethodsComposable() {
    SettlementMethodsComposable()
}