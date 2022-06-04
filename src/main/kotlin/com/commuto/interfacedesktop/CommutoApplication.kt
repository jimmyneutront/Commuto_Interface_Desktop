package com.commuto.interfacedesktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.ui.OffersComposable
import com.commuto.interfacedesktop.ui.OffersViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommutoApplication @Inject constructor(
    val blockchainService: BlockchainService,
    val offersViewModel: OffersViewModel
) {
    fun start() {
        blockchainService.listen()
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
}