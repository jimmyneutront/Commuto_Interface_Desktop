package com.commuto.interfacedesktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.p2p.P2PService
import com.commuto.interfacedesktop.ui.OffersComposable
import com.commuto.interfacedesktop.ui.OffersViewModel
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
            Window(
                onCloseRequest = ::exitApplication,
                title = "Compose for Desktop",
                state = rememberWindowState(width = 700.dp, height = 600.dp)
            ) {
                OffersComposable(offersViewModel)
            }
        }
    }
}