package com.commuto.interfacedesktop.offer.validation

import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformation
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
        val validatedData = validateNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigDecimal("100.0"),
            maximumAmount = BigDecimal("200.0"),
            securityDepositAmount = BigDecimal("20.0"),
            serviceFeeRate = BigInteger("100"),
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                SettlementMethod(currency = "a_currency", price = "a_price", method = "a_settlement_method")
            ),
        )
        val expectedValidatedData = ValidatedNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigInteger("100000000000000000000"),
            maximumAmount = BigInteger("200000000000000000000"),
            securityDepositAmount = BigInteger("20000000000000000000"),
            serviceFeeRate = BigInteger("100"),
            serviceFeeAmountLowerBound = BigInteger("1000000000000000000"),
            serviceFeeAmountUpperBound = BigInteger("2000000000000000000"),
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                SettlementMethod(currency = "a_currency", price = "a_price", method = "a_settlement_method")
            )
        )
        assertEquals(expectedValidatedData, validatedData)
    }

    /**
     * Ensures that [validateNewOfferData] validates offer data properly when the service fee rate is zero.
     */
    @Test
    fun testValidateNewOfferDataZeroServiceFee() {
        val validatedData = validateNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigDecimal("100.0"),
            maximumAmount = BigDecimal("200.0"),
            securityDepositAmount = BigDecimal("20.0"),
            serviceFeeRate = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                SettlementMethod(currency = "a_currency", price = "a_price", method = "a_settlement_method")
            ),
        )
        val expectedValidatedData = ValidatedNewOfferData(
            stablecoin = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            stablecoinInformation = StablecoinInformation(currencyCode = "DAI", name = "Dai", decimal = 18),
            minimumAmount = BigInteger("100000000000000000000"),
            maximumAmount = BigInteger("200000000000000000000"),
            securityDepositAmount = BigInteger("20000000000000000000"),
            serviceFeeRate = BigInteger.ZERO,
            serviceFeeAmountLowerBound = BigInteger.ZERO,
            serviceFeeAmountUpperBound = BigInteger.ZERO,
            direction = OfferDirection.BUY,
            settlementMethods = listOf(
                SettlementMethod(currency = "a_currency", price = "a_price", method = "a_settlement_method")
            )
        )
        assertEquals(expectedValidatedData, validatedData)
    }

}