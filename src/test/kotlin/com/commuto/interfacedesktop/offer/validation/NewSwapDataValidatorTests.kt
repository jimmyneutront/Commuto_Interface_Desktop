package com.commuto.interfacedesktop.offer.validation

import androidx.compose.runtime.mutableStateListOf
import com.commuto.interfacedesktop.offer.Offer
import com.commuto.interfacedesktop.offer.OfferDirection
import com.commuto.interfacedesktop.offer.OfferState
import com.commuto.interfacedesktop.settlement.SettlementMethod
import com.commuto.interfacedesktop.settlement.privatedata.PrivateSWIFTData
import com.commuto.interfacedesktop.ui.StablecoinInformationRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        val makerSettlementMethod = SettlementMethod(
            currency = "USD",
            price = "1.00",
            method = "SWIFT",
            privateData = Json.encodeToString(PrivateSWIFTData(
                accountHolder = "account_holder",
                bic = "bic",
                accountNumber = "account_number"
            ))
        )
        val takerSettlementMethod = SettlementMethod(
            currency = "USD",
            price = "",
            method = "SWIFT",
            privateData = Json.encodeToString(PrivateSWIFTData(
                accountHolder = "account_holder",
                bic = "bic",
                accountNumber = "account_number"
            ))
        )
        val offer = Offer(
            isCreated = true,
            isTaken = false,
            id = UUID.randomUUID(),
            maker = "0x0000000000000000000000000000000000000000",
            interfaceID = ByteArray(0),
            stablecoin = "0x8438Ad1C834623CfF278AB6829a248E37C2D7E3f", // BUSD on Hardhat,
            amountLowerBound = BigInteger.valueOf(10) * BigInteger.TEN.pow(18),
            amountUpperBound = BigInteger.valueOf(20) * BigInteger.TEN.pow(18),
            securityDepositAmount = BigInteger.valueOf(2) * BigInteger.TEN.pow(18),
            serviceFeeRate = BigInteger.valueOf(100),
            direction = OfferDirection.BUY,
            settlementMethods = mutableStateListOf(makerSettlementMethod),
            protocolVersion = BigInteger.valueOf(1),
            chainID = BigInteger.valueOf(31337),
            havePublicKey = true,
            isUserMaker = false,
            state = OfferState.OFFER_OPENED
        )
        val validatedSwapData = validateNewSwapData(
            offer = offer,
            takenSwapAmount = BigDecimal(15),
            selectedMakerSettlementMethod = makerSettlementMethod,
            selectedTakerSettlementMethod = takerSettlementMethod,
            stablecoinInformationRepository = StablecoinInformationRepository.hardhatStablecoinInfoRepo
        )
        val expectedValidatedSwapData = ValidatedNewSwapData(
            takenSwapAmount = BigInteger.valueOf(15) * BigInteger.TEN.pow(18),
            makerSettlementMethod = makerSettlementMethod,
            takerSettlementMethod = takerSettlementMethod
        )
        assertEquals(expectedValidatedSwapData, validatedSwapData)
    }
}