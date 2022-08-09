package com.commuto.interfacedesktop

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.ui.CurrentTab
import com.commuto.interfacedesktop.ui.offer.OffersViewModel
import com.commuto.interfacedesktop.ui.TabSidebarComposable
import com.commuto.interfacedesktop.ui.offer.OffersComposable
import com.commuto.interfacedesktop.ui.swap.SwapsComposable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Commuto Interface application.
 *
 * @property blockchainService The app's [BlockchainService].
 * @property offersViewModel The app's [OffersViewModel].
 */
@Singleton
class CommutoApplication @Inject constructor(
    val databaseService: DatabaseService,
    val blockchainService: BlockchainService,
    val p2pService: P2PService,
    val offersViewModel: OffersViewModel
) {
    /**
     * Called at app startup. This begins background activities and displays the user interface.
     */
    fun start() {
        // TODO: ONLY do this when using an in memory database, NOT when using a production database!
        databaseService.createTables()
        // Start listening to the blockchain
        blockchainService.listen()
        // Start listening to the peer-to-peer network
        p2pService.listen()
        application {

            val currentTab = remember { mutableStateOf(CurrentTab.OFFERS) }

            Window(
                onCloseRequest = ::exitApplication,
                title = "Compose for Desktop",
                state = rememberWindowState(width = 900.dp, height = 600.dp)
            ) {
                Row {
                    TabSidebarComposable(
                        currentTab = currentTab
                    )
                    when (currentTab.value) {
                        CurrentTab.OFFERS -> {
                            OffersComposable(offersViewModel)
                        }
                        CurrentTab.SWAPS -> {
                            SwapsComposable()
                        }
                    }
                }
            }
        }
    }
}