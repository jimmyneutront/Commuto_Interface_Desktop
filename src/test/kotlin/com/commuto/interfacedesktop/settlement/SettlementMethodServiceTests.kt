package com.commuto.interfacedesktop.settlement

import com.commuto.interfacedesktop.database.DatabaseDriverFactory
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.ui.settlement.PreviewableSettlementMethodTruthSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [SettlementMethodService]
 */
class SettlementMethodServiceTests {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * Ensures [SettlementMethodService.addSettlementMethod] functions properly.
     */
    @Test
    fun testAddSettlementMethod() = runBlocking {

        val databaseService = DatabaseService(DatabaseDriverFactory())
        databaseService.createTables()

        val settlementMethodTruthSource = PreviewableSettlementMethodTruthSource()
        settlementMethodTruthSource.settlementMethods.clear()

        val settlementMethodService = SettlementMethodService(
            databaseService = databaseService
        )
        settlementMethodService.setSettlementMethodTruthSource(newTruthSource = settlementMethodTruthSource)

        val settlementMethodToAdd = SettlementMethod(
            currency = "EUR",
            price = "",
            method = "SEPA"
        )
        val privateData = PrivateSEPAData(
            accountHolder = "accountHolder",
            bic = "bic",
            iban = "iban",
            address = "address",
        )

        settlementMethodService.addSettlementMethod(
            settlementMethod = settlementMethodToAdd,
            newPrivateData = privateData
        )

        assertEquals(1, settlementMethodTruthSource.settlementMethods.size)
        val addedSettlementMethod = settlementMethodTruthSource.settlementMethods.first()
        assertEquals("EUR", addedSettlementMethod.currency)
        assertEquals("SEPA", addedSettlementMethod.method)
        assertEquals(Json.encodeToString(privateData), addedSettlementMethod.privateData)

    }

}