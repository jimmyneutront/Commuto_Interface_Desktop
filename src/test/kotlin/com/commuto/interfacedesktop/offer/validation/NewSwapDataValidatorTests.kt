package com.commuto.interfacedesktop.offer.validation

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OfferState
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class NewSwapDataValidatorTests {

    /**
     * Ensures that [validateNewSwapData] validates swap data properly.
     */
    @Test
    fun testValidateNewSwapData() {
        val settlementMethod = SettlementMethod(
            currency = "a_currency",
            price = "a_price",
            method = "a_settlement_method",
            privateData = "some_private_data"
        )
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = UUID.randomUUID(),
            maker = "0x0000000000000000000000000000000000000000",
            interfaceId = ByteArray(0),
            stablecoin = "0x8438Ad1C834623CfF278AB6829a248E37C2D7E3f", // BUSD on Hardhat,
            amountLowerBound = BigInteger.valueOf(10) * BigInteger.TEN.pow(18),
            amountUpperBound = BigInteger.valueOf(20) * BigInteger.TEN.pow(18),
            securityDepositAmount = BigInteger.valueOf(2) * BigInteger.TEN.pow(18),
            serviceFeeRate = BigInteger.valueOf(100),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(settlementMethod),
            protocolVersion = BigInteger.valueOf(1),
            chainID = BigInteger.valueOf(31337),
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )
        val validatedSwapData = validateNewSwapData(
            offer = offer,
            takenSwapAmount = BigDecimal(15),
            selectedSettlementMethod = settlementMethod,
            stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
        )
        val expectedValidatedSwapData = ValidatedNewSwapData(
            takenSwapAmount = BigInteger.valueOf(15) * BigInteger.TEN.pow(18),
            settlementMethod = settlementMethod
        )
        assertEquals(expectedValidatedSwapData, validatedSwapData)
    }
}