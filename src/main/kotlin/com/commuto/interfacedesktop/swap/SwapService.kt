package com.commuto.interfacedesktop.swap

import com.commuto.interfacedesktop.blockchain.BlockchainService
import com.commuto.interfacedesktop.database.DatabaseService
import com.commuto.interfacedesktop.extension.asByteArray
import com.commuto.interfacedesktop.key.KeyManagerService
import com.commuto.interfacedesktop.offer.OfferService
import com.commuto.interfacedesktop.p2p.P2PService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The main Swap Service. It is responsible for processing and organizing swap-related data that it receives from
 * [BlockchainService], [P2PService] and [OfferService] in order to maintain an accurate list of all swaps in a
 * [SwapTruthSource].
 *
 * @property databaseService The [DatabaseService] that this [SwapService] uses for persistent storage.
 * @property keyManagerService The [KeyManagerService] that this [SwapService] will use for managing keys.
 * @property logger The [org.slf4j.Logger] that this class uses for logging.
 */
@Singleton
class SwapService @Inject constructor(
    private val databaseService: DatabaseService,
    private val keyManagerService: KeyManagerService,
): SwapNotifiable {

    private lateinit var swapTruthSource: SwapTruthSource

    private lateinit var p2pService: P2PService

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Used to set the [swapTruthSource] property. This can only be called once.
     *
     * @param newTruthSource The new value of the [swapTruthSource] property, which cannot be null.
     */
    fun setSwapTruthSource(newTruthSource: SwapTruthSource) {
        check(!::swapTruthSource.isInitialized) {
            "swapTruthSource is already initialized"
        }
        swapTruthSource = newTruthSource
    }

    /**
     * Used to set the [p2pService] property. This can only be called once.
     *
     * @param newP2PService The new value of the [p2pService] property, which cannot be null.
     */
    fun setP2PService(newP2PService: P2PService) {
        check(!::p2pService.isInitialized) {
            "p2pService is already initialized"
        }
        p2pService = newP2PService
    }

    /**
     * Sends a
     * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
     * for an offer taken by the user of this interface, with an ID equal to [swapID] and a blockchain ID equal to
     * [chainID].
     *
     * This updates the state of the swap with the specified ID (both persistently and in [swapTruthSource]) to
     * [SwapState.AWAITING_TAKER_INFORMATION], creates and sends a Taker Information Message, and then updates the state
     * of the swap with the specified ID (both persistently and in [swapTruthSource]) to
     * [SwapState.AWAITING_MAKER_INFORMATION].
     *
     * @param swapID The ID of the swap for which taker information will be sent.
     * @param chainID The ID of the blockchain on which the swap exists.
     */
    override suspend fun sendTakerInformationMessage(swapID: UUID, chainID: BigInteger) {
        logger.info("announceTakerInformation: preparing to announce for ${swapID}")
        val encoder = Base64.getEncoder()
        val swapIDString = encoder.encodeToString(swapID.asByteArray())
        databaseService.updateSwapState(
            swapID = swapIDString,
            chainID = chainID.toString(),
            state = SwapState.AWAITING_TAKER_INFORMATION.asString
        )
        withContext(Dispatchers.Main) {
            swapTruthSource.swaps[swapID]?.state = SwapState.AWAITING_TAKER_INFORMATION
        }
        // Since we are the taker of this swap, we should have it in persistent storage
        val swapInDatabase = databaseService.getSwap(id = swapIDString)
        if (swapInDatabase == null) {
            throw SwapServiceException("Could not find swap $swapID in persistent storage")
        }
        val decoder = Base64.getDecoder()
        val makerInterfaceID = try {
            decoder.decode(swapInDatabase.makerInterfaceID)
        } catch (exception: Exception) {
            throw SwapServiceException("Unable to get maker interface ID for $swapID. makerInterfaceID string: " +
                    swapInDatabase.makerInterfaceID
            )
        }
        /*
        The user of this interface has taken the swap. Since taking a swap is not allowed unless we have a copy of the
        maker's public key, we should have said public key in storage.
         */
        val makerPublicKey = keyManagerService.getPublicKey(makerInterfaceID)
        if (makerPublicKey == null) {
            throw SwapServiceException("Could not find maker's public key for $swapID")
        }
        val takerInterfaceID = try {
            decoder.decode(swapInDatabase.takerInterfaceID)
        } catch (exception: Exception) {
            throw SwapServiceException("Unable to get taker interface ID for $swapID. takerInterfaceID string: " +
                    swapInDatabase.takerInterfaceID)
        }
        /*
        Since the user of this interface has taken the swap, we should have a key pair for the swap in persistent
        storage.
         */
        val takerKeyPair = keyManagerService.getKeyPair(takerInterfaceID)
        if (takerKeyPair == null) {
            throw SwapServiceException("Could not find taker's (user's) key pair for $swapID")
        }
        // TODO: get actual payment details once settlementMethodService is implemented
        val settlementMethodDetailsString = "TEMPORARY"
        logger.info("announceTakerInformation: announcing for $swapID")
        p2pService.sendTakerInformation(
            makerPublicKey = makerPublicKey,
            takerKeyPair = takerKeyPair,
            swapID = swapID,
            settlementMethodDetails = settlementMethodDetailsString
        )
        logger.info("announceTakerInformation: announced for $swapID")
        databaseService.updateSwapState(
            swapID = swapIDString,
            chainID = chainID.toString(),
            state = SwapState.AWAITING_MAKER_INFORMATION.asString
        )
        withContext(Dispatchers.Main) {
            swapTruthSource.swaps[swapID]?.state = SwapState.AWAITING_MAKER_INFORMATION
        }
    }
}