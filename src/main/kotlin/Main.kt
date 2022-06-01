// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.commuto.interfacedesktop.ui.OffersComposable
import com.commuto.interfacedesktop.ui.OffersViewModel


fun main() {

    val offersViewModel = OffersViewModel()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Compose for Desktop",
            state = rememberWindowState(width = 500.dp, height = 300.dp)
        ) {
            OffersComposable(offersViewModel)
        }
    }
}