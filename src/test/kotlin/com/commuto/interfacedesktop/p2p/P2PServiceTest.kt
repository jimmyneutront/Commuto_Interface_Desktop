package com.commuto.interfacedesktop.p2p

import kotlinx.coroutines.runBlocking
import org.junit.Test

class P2PServiceTest {
    @Test
    fun testP2PService() = runBlocking {
        val p2pService = P2PService()
        p2pService.listenLoop()
    }
}