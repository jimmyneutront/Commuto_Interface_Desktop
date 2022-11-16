package com.commuto.interfacedesktop.ui.settlement

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Displays the list of the user's settlement methods as [SettlementMethodCardComposable]s in a [LazyColumn].
 */
@Composable
fun SettlementMethodsComposable() {

    /**
     * Indicates which composable should be shown on the trailing side of the list of [SettlementMethodCardComposable]s
     */
    val focusedSettlementMethodComposable = remember {
        mutableStateOf(FocusedSettlementMethodComposable.SettlementMethodComposable)
    }

    /**
     * The settlement method to show in the settlement method detail composable.
     */
    val focusedSettlementMethod = remember { mutableStateOf<SettlementMethod?>(null) }

    /**
     * Private data of a [SettlementMethod] that is or was the focused settlement method, or `null` if no such
     * [PrivateData] exists. (Note that this may not necessarily contain the private data of `focusedSettlementMethod`,
     * but it will be updated with the private data of `focusedSettlementMethod` every time the
     * settlement-method-editing Composable is displayed.
     */
    val privateData = remember { mutableStateOf<PrivateData?>(null) }

    /**
     * The list of the user's current settlement methods.
     */
    val settlementMethods = remember {
        mutableStateListOf<SettlementMethod>().also { mutableStateList ->
            SettlementMethod.sampleSettlementMethodsEmptyPrices.map {
                mutableStateList.add(it)
            }
        }
    }

    Row {
        Column(
            modifier = Modifier.widthIn(100.dp, 300.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 10.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settlement Methods",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = {
                        focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                            .AddSettlementMethodComposable
                    },
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
                for (entry in settlementMethods) {
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
                        settlementMethod = focusedSettlementMethod.value,
                        settlementMethods = settlementMethods,
                        focusedSettlementMethod = focusedSettlementMethod,
                        focusedSettlementMethodComposable = focusedSettlementMethodComposable
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
            FocusedSettlementMethodComposable.AddSettlementMethodComposable -> {
                AddSettlementMethodComposable(
                    focusedSettlementMethodComposable = focusedSettlementMethodComposable,
                    focusedSettlementMethod = focusedSettlementMethod,
                    settlementMethods = settlementMethods
                )
            }
            FocusedSettlementMethodComposable.EditSettlementMethodComposable -> {
                EditSettlementMethodComposable(
                    focusedSettlementMethod = focusedSettlementMethod,
                    privateData = privateData,
                    focusedSettlementMethodComposable = focusedSettlementMethodComposable,
                )
            }
        }
    }
}

/**
 * An enum representing the type of settlement method that the user has decided to create.
 */
enum class SettlementMethodType(val description: String) {
    SEPA("SEPA"), SWIFT("SWIFT")
}

/**
 * A [Composable] by which the user can add settlement methods.
 * @param focusedSettlementMethodComposable A [MutableState] wrapped around an enum representing the currently focused
 * settlement method Composable, the value of which this will set to
 * [FocusedSettlementMethodComposable.SettlementMethodComposable] once the settlement method adding process is
 * complete.
 * @param focusedSettlementMethod A [MutableState] wrapped around the currently focused [SettlementMethod].
 * @param settlementMethods A [SnapshotStateList] containing the user's [SettlementMethod]s.
 */
@Composable
fun AddSettlementMethodComposable(
    focusedSettlementMethodComposable: MutableState<FocusedSettlementMethodComposable>,
    focusedSettlementMethod: MutableState<SettlementMethod?>,
    settlementMethods: SnapshotStateList<SettlementMethod>
) {

    val selectedSettlementMethod = remember { mutableStateOf<SettlementMethodType?>(null) }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Add Settlement Method",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.75f)
            )
            Button(
                onClick = {
                    focusedSettlementMethod.value = null
                    focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                        .SettlementMethodComposable
                },
                content = {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                border = BorderStroke(1.dp, Color.Black),
                elevation = null,
            )
        }
        for (settlementMethodType in SettlementMethodType.values()) {
            Button(
                onClick = {
                    selectedSettlementMethod.value = settlementMethodType
                },
                content = {
                    Text(
                        text = settlementMethodType.description,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color.Transparent,
                    contentColor = Color.Black,
                ),
                border = BorderStroke(1.dp, getColorForSettlementMethod(
                    settlementMethodType = settlementMethodType,
                    selectedSettlementMethod = selectedSettlementMethod
                )),
                contentPadding = PaddingValues(15.dp),
                elevation = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
        if (selectedSettlementMethod.value == SettlementMethodType.SEPA) {
            EditableSEPADetailComposable(
                buttonText = "Add",
                buttonAction = { newPrivateData ->
                    val newSettlementMethod = SettlementMethod(
                        currency = "EUR",
                        method = "SEPA",
                        price = ""
                    )
                    try {
                        newSettlementMethod.privateData = Json.encodeToString(newPrivateData as PrivateSEPAData)
                        settlementMethods.add(newSettlementMethod)
                    } catch (_: Exception) {}
                    focusedSettlementMethod.value = newSettlementMethod
                    focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                        .SettlementMethodComposable
                }
            )
        } else if (selectedSettlementMethod.value == SettlementMethodType.SWIFT) {
            EditableSWIFTDetailComposable(
                buttonText = "Add",
                buttonAction = { newPrivateData ->
                    val newSettlementMethod = SettlementMethod(
                        currency = "USD",
                        method = "SWIFT",
                        price = ""
                    )
                    try {
                        newSettlementMethod.privateData = Json.encodeToString(newPrivateData as PrivateSWIFTData)
                        settlementMethods.add(newSettlementMethod)
                    } catch (_: Exception) {}
                    focusedSettlementMethod.value = newSettlementMethod
                    focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                        .SettlementMethodComposable
                }
            )
        }
    }
}

/**
 * Returns the proper color for a card displaying a settlement method type in [AddSettlementMethodComposable]:
 * [Color.Green] if [settlementMethodType] equals the value of [selectedSettlementMethod], and [Color.Black] otherwise.
 *
 * @param settlementMethodType The type of settlement method of the card for which this computes the proper color.
 * @param selectedSettlementMethod The type of settlement method that the user has selected.
 */
fun getColorForSettlementMethod(
    settlementMethodType: SettlementMethodType,
    selectedSettlementMethod: MutableState<SettlementMethodType?>
): Color {
    return if (selectedSettlementMethod.value == settlementMethodType) {
        Color.Green
    } else {
        Color.Black
    }
}

/**
 * Allows the user to supply private SEPA data. When the user presses the "Done" button, a new [PrivateSEPAData] is
 * created from the data they have supplied, and is passed to [buttonAction].
 *
 * @param buttonText The label of the button that lies below the input text fields.
 * @param buttonAction The action that the button should perform when clicked, which receives an object implementing
 * [PrivateData] made from the data supplied by the user.
 */
@Composable
fun EditableSEPADetailComposable(
    buttonText: String,
    buttonAction: (PrivateData) -> Unit,
) {
    val accountHolder = remember { mutableStateOf("") }
    val bic = remember { mutableStateOf("") }
    val iban = remember { mutableStateOf("") }
    val address = remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Account Holder:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = accountHolder.value,
            onValueChange = { accountHolder.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "BIC:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = bic.value,
            onValueChange = { bic.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "IBAN:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = iban.value,
            onValueChange = { iban.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Address:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = address.value,
            onValueChange = { address.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                buttonAction(PrivateSEPAData(
                    accountHolder = accountHolder.value,
                    bic = bic.value,
                    iban = iban.value,
                    address = address.value)
                )
            },
            content = {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                )
            },
            border = BorderStroke(3.dp, Color.Black),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =  Color.Transparent,
                contentColor = Color.Black,
            ),
            elevation = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

/**
 * Allows the user to supply private SWIFT data. When the user presses the "Done" button, a new [PrivateSWIFTData] is
 * created from the data they have supplied, and is passed to [buttonAction].
 *
 * @param buttonText The label of the button that lies below the input text fields.
 * @param buttonAction The action that the button should perform when clicked, which receives an object implementing
 * [PrivateData] made from the data supplied by the user.
 */
@Composable
fun EditableSWIFTDetailComposable(
    buttonText: String,
    buttonAction: (PrivateData) -> Unit,
) {
    val accountHolder = remember { mutableStateOf("") }
    val bic = remember { mutableStateOf("") }
    val accountNumber = remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Account Holder:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = accountHolder.value,
            onValueChange = { accountHolder.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "BIC:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = bic.value,
            onValueChange = { bic.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Account Number:",
            style = MaterialTheme.typography.h5,
        )
        TextField(
            value = accountNumber.value,
            onValueChange = { accountNumber.value = it },
            textStyle = MaterialTheme.typography.h4,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                buttonAction(PrivateSWIFTData(
                    accountHolder = accountHolder.value,
                    bic = bic.value,
                    accountNumber = accountNumber.value)
                )
            },
            content = {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                )
            },
            border = BorderStroke(3.dp, Color.Black),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =  Color.Transparent,
                contentColor = Color.Black,
            ),
            elevation = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

/**
 * A card displaying basic information about a settlement method belonging to the user, to be shown in the list of the
 * user's settlement methods.
 * @param settlementMethod The [SettlementMethod] about which information will be displayed.
 */
@Composable
fun SettlementMethodCardComposable(settlementMethod: SettlementMethod) {
    /**
     * An object implementing [PrivateData] containing private data for [settlementMethod].
     */
    val privateData = remember { mutableStateOf<PrivateData?>(null) }

    /**
     * Indicates whether we have finished attempting to parse the private data associated with [settlementMethod].
     */
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
 *
 * @param settlementMethod The [SettlementMethod] containing the information to be displayed.
 * @param settlementMethods A [SnapshotStateList] of the user's current [SettlementMethod]s.
 * @param focusedSettlementMethod A [MutableState] wrapped around the currently focused settlement method, the value of
 * which this will set to null if the user deletes the settlement method
 */
@Composable
fun SettlementMethodDetailComposable(
    settlementMethod: SettlementMethod?,
    settlementMethods: SnapshotStateList<SettlementMethod>,
    focusedSettlementMethod: MutableState<SettlementMethod?>,
    focusedSettlementMethodComposable: MutableState<FocusedSettlementMethodComposable>,
) {
    /**
     * An object implementing [PrivateData] containing private data for [settlementMethod].
     */
    val privateData = remember { mutableStateOf<PrivateData?>(null) }
    /**
     * Indicates whether we have finished attempting to parse the private data associated with [settlementMethod].
     */
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
            Button(
                onClick = {
                    focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                        .EditSettlementMethodComposable
                },
                content = {
                    Text(
                        text = "Edit",
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
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .fillMaxWidth(),
            )
            Button(
                onClick = {
                    settlementMethods.removeAll {
                        it.method == settlementMethod.method
                                && it.currency == settlementMethod.currency
                                && it.privateData == settlementMethod.privateData
                    }
                    focusedSettlementMethod.value = null
                },
                content = {
                    Text(
                        text = "Delete",
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
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .fillMaxWidth(),
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
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
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    privateData.value = null
                }
            }
            try {
                val privateSWIFTData = Json.decodeFromString<PrivateSWIFTData>(privateDataString)
                withContext(Dispatchers.Main) {
                    privateData.value = privateSWIFTData
                    finishedParsingData.value = true
                }
                return@withContext
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    privateData.value = null
                }
            }
        }
        finishedParsingData.value = true
        return@withContext
    }
}

/**
 * Displays a [Composable] allowing the user to edit the private data of the [SettlementMethod] around which
 * [focusedSettlementMethod] is wrapped, or displays an error * message if the details cannot be edited.
 * @param focusedSettlementMethod A [MutableState] wrapped around a [SettlementMethod], the private data of which this
 * edits.
 * @param privateData A [MutableState] containing an object implementing [PrivateData] for [focusedSettlementMethod].
 * @param focusedSettlementMethodComposable A [MutableState] wrapped around an enum representing the currently focused
 * settlement method Composable
 */
@Composable
fun EditSettlementMethodComposable(
    focusedSettlementMethod: MutableState<SettlementMethod?>,
    privateData: MutableState<PrivateData?>,
    focusedSettlementMethodComposable: MutableState<FocusedSettlementMethodComposable>
) {
    if (focusedSettlementMethod.value != null) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Edit ${focusedSettlementMethod.value?.method ?: ""} Details",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.75f)
                )
                Button(
                    onClick = {
                        focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                            .SettlementMethodComposable
                    },
                    content = {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor =  Color.Transparent,
                        contentColor = Color.Black,
                    ),
                    border = BorderStroke(1.dp, Color.Black),
                    elevation = null,
                )
            }
            when (focusedSettlementMethod.value?.method ?: "") {
                "SEPA" -> {
                    EditableSEPADetailComposable(
                        buttonText = "Done",
                        buttonAction = { newPrivateData ->
                            privateData.value = newPrivateData
                            focusedSettlementMethod.value?.privateData = Json.encodeToString(
                                newPrivateData as PrivateSEPAData
                            )
                            focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                                .SettlementMethodComposable
                        }
                    )
                }
                "SWIFT" -> {
                    EditableSWIFTDetailComposable(
                        buttonText = "Done",
                        buttonAction = { newPrivateData ->
                            privateData.value = newPrivateData
                            focusedSettlementMethod.value?.privateData = Json.encodeToString(
                                newPrivateData as PrivateSWIFTData
                            )
                            focusedSettlementMethodComposable.value = FocusedSettlementMethodComposable
                                .SettlementMethodComposable
                        }
                    )
                }
                else -> {
                    Text(
                        text = "Unable to edit details"
                    )
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "No Settlement Method Selected.",
            )
        }
    }
}

@Preview
@Composable
fun PreviewSettlementMethodsComposable() {
    SettlementMethodsComposable()
}