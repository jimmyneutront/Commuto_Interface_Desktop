package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSEPAData
import com.commuto.interfacedesktop.ui.StablecoinInformation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class NewOfferDataValidatorTests {

    /**
     * Ensures that [validateNewOfferData] validates offer data properly.
     */
    @Test
    fun testValidateNewOfferData() {
        val settlementMethod = SettlementMethod(
            currency = "EUR",
            price = "0.98",
            method = "SEPA"
        )
        settlementMethod.privateData = Json.encodeToString(
            PrivateSEPAData(
                accountHolder = "account_holder",
                bic = "bic",
                iban = "iban",
                address = "address",
            )
        )
        val validatedData = validateNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigDecimal("100.0"),
            maximumAmount = BigDecimal("200.0"),
            securityDepositAmount = BigDecimal("20.0"),
            serviceFeeRate = BigInteger("100"),
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                settlementMethod
            ),
        )
        assertEquals("0x6B175474E89094C44Da98b954EedeAC495271d0F", validatedData.stablecoin)
        assertEquals(
            StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            validatedData.stablecoinInformation
        )
        assertEquals(BigInteger("100000000000000000000"), validatedData.minimumAmount)
        assertEquals(BigInteger("200000000000000000000"), validatedData.maximumAmount)
        assertEquals(BigInteger("20000000000000000000"), validatedData.securityDepositAmount)
        assertEquals(BigInteger("100"), validatedData.serviceFeeRate)
        assertEquals(BigInteger("1000000000000000000"), validatedData.serviceFeeAmountLowerBound)
        assertEquals(BigInteger("2000000000000000000"), validatedData.serviceFeeAmountUpperBound)
        assertEquals(OfferDirection.BUY, validatedData.direction)
        assertEquals(1, validatedData.settlementMethods.size)
        assertEquals("EUR", validatedData.settlementMethods.first().currency)
        assertEquals("0.98", validatedData.settlementMethods.first().price)
        assertEquals("SEPA", validatedData.settlementMethods.first().method)
        assertEquals(settlementMethod.privateData, validatedData.settlementMethods.first().privateData)
    }

    /**
     * Ensures that [validateNewOfferData] validates offer data properly when the service fee rate is zero.
     */
    @Test
    fun testValidateNewOfferDataZeroServiceFee() {
        val settlementMethod = SettlementMethod(
            currency = "EUR",
            price = "0.98",
            method = "SEPA"
        )
        settlementMethod.privateData = Json.encodeToString(
            PrivateSEPAData(
                accountHolder = "account_holder",
                bic = "bic",
                iban = "iban",
                address = "address",
            )
        )
        val validatedData = validateNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigDecimal("100.0"),
            maximumAmount = BigDecimal("200.0"),
            securityDepositAmount = BigDecimal("20.0"),
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                settlementMethod
            ),
        )
        assertEquals("0x6B175474E89094C44Da98b954EedeAC495271d0F", validatedData.stablecoin)
        assertEquals(
            StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            validatedData.stablecoinInformation
        )
        assertEquals(BigInteger("100000000000000000000"), validatedData.minimumAmount)
        assertEquals(BigInteger("200000000000000000000"), validatedData.maximumAmount)
        assertEquals(BigInteger("20000000000000000000"), validatedData.securityDepositAmount)
        assertEquals(BigInteger.ZERO, validatedData.serviceFeeRate)
        assertEquals(BigInteger.ZERO, validatedData.serviceFeeAmountLowerBound)
        assertEquals(BigInteger.ZERO, validatedData.serviceFeeAmountUpperBound)
        assertEquals(OfferDirection.BUY, validatedData.direction)
        assertEquals(1, validatedData.settlementMethods.size)
        assertEquals("EUR", validatedData.settlementMethods.first().currency)
        assertEquals("0.98", validatedData.settlementMethods.first().price)
        assertEquals("SEPA", validatedData.settlementMethods.first().method)
        assertEquals(settlementMethod.privateData, validatedData.settlementMethods.first().privateData)
    }

}