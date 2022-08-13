package com.commuto.interfacedesktop.offer

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.util.*

class OfferTests {

    /**
     * Ensures that [Offer.updateSettlementMethods] updates [Offer.settlementMethods] and
     * [Offer.onChainSettlementMethods] properly.
     */
    @Test
    fun testUpdateSettlementMethods() {
        val settlementMethod = SettlementMethod(
            currency = "EUR",
            method = "SEPA",
            price = "0.98",
        )
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = UUID.randomUUID(),
            maker = "maker_address",
            interfaceId = ByteArray(0),
            stablecoin = "stablecoin_address",
            amountLowerBound = BigInteger.ONE,
            amountUpperBound = BigInteger.ONE,
            securityDepositAmount = BigInteger.ONE,
            serviceFeeRate = BigInteger.ONE,
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(
                settlementMethod
            ),
            protocolVersion = BigInteger.ONE,
            chainID = BigInteger.ONE,
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )
        val newSettlementMethod = SettlementMethod(
            currency = "USD",
            method = "SWIFT",
            price = "1.00"
        )
        offer.updateSettlementMethods(
            settlementMethods = listOf(
                newSettlementMethod
            )
        )
        assertEquals(offer.settlementMethods.size, 1)
        assertEquals(offer.settlementMethods[0], newSettlementMethod)
        assertEquals(offer.onChainSettlementMethods.size, 1)
        assert(offer.onChainSettlementMethods[0].contentEquals(
            Json.encodeToString(newSettlementMethod).encodeToByteArray()))
    }

    /**
     * Ensures that [Offer.updateSettlementMethodsFromChain] updates [Offer.settlementMethods] and
     * [Offer.onChainSettlementMethods] properly.
     */
    @Test
    fun testUpdateSettlementMethodsFromChain() {
        val settlementMethod = SettlementMethod(
            currency = "EUR",
            method = "SEPA",
            price = "0.98",
        )
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = UUID.randomUUID(),
            maker = "maker_address",
            interfaceId = ByteArray(0),
            stablecoin = "stablecoin_address",
            amountLowerBound = BigInteger.ONE,
            amountUpperBound = BigInteger.ONE,
            securityDepositAmount = BigInteger.ONE,
            serviceFeeRate = BigInteger.ONE,
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(
                settlementMethod
            ),
            protocolVersion = BigInteger.ONE,
            chainID = BigInteger.ONE,
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )
        val newSettlementMethod = SettlementMethod(
            currency = "USD",
            method = "SWIFT",
            price = "1.00"
        )
        offer.updateSettlementMethodsFromChain(
            onChainSettlementMethods = listOf(
                Json.encodeToString(newSettlementMethod)
                    .encodeToByteArray()
            )
        )
        assertEquals(offer.settlementMethods.size, 1)
        assertEquals(offer.settlementMethods[0], newSettlementMethod)
        assertEquals(offer.onChainSettlementMethods.size, 1)
        assert(offer.onChainSettlementMethods[0].contentEquals(Json.encodeToString(newSettlementMethod)
            .encodeToByteArray()))
    }
}