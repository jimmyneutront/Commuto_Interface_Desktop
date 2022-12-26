package com.commuto.interfacedesktop.database

import com.commuto.interfacedesktop.db.*

/**
 * A wrapper around the [CommutoInterfaceDB] class, which is auto-generated by SQLDelight.
 * @property database The [CommutoInterfaceDB] object that this class wraps.
 * @property dbQuery The [com.commuto.interfacedesktop.db.CommutoInterfaceDBQueries] property of [database], used for
 * executing database queries.
 */
internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CommutoInterfaceDB(databaseDriverFactory.createDriver())
    private val dbQuery = database.commutoInterfaceDBQueries

    /**
     * Creates all necessary database tables.
     */
    internal fun createTables() {
        dbQuery.createOfferTable()
        dbQuery.createSettlementMethodTable()
        dbQuery.createPendingOfferSettlementMethodTable()
        dbQuery.createPublicKeyTable()
        dbQuery.createKeyPairTable()
        dbQuery.createSwapTable()
        dbQuery.createUserSettlementMethodTable()
    }

    /**
     * Deletes everything from every database table.
     */
    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.removeAllOffers()
            dbQuery.removeAllOfferSettlementMethods()
            dbQuery.removeAllPendingOfferSettlementMethods()
            dbQuery.removeAllKeyPairs()
            dbQuery.removeAllPublicKeys()
            dbQuery.removeAllSwaps()
            dbQuery.removeAllUserSettlementMethods()
        }
    }

    /**
     * Returns [Offer](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#offer)s with the specified offer
     * ID.
     * @param id The ID of the Offers to be returned.
     * @return A [List] of [Offer]s with offer IDs equal to [id].
     */
    internal fun selectOfferByOfferId(id: String): List<Offer> {
        return dbQuery.selectOfferByOfferId(id).executeAsList()
    }

    /**
     * Returns [OfferSettlementMethod]s with the specified offer ID and blockchain ID.
     * @param offerID The offer ID associated with the settlement methods to be returned.
     * @param chainID The blockchain ID associated with the settlement methods to be returned.
     * @return A [List] of [OfferSettlementMethod]s
     */
    internal fun selectOfferSettlementMethodByOfferIdAndChainID(
        offerID: String,
        chainID: String
    ): List<OfferSettlementMethod> {
        return dbQuery.selectOfferSettlementMethodByOfferIdAndChainID(offerID, chainID).executeAsList()
    }

    /**
     * Returns pending [OfferSettlementMethod]s with the specified offer ID and blockchain ID.
     * @param offerID The offer ID associated with the pending settlement methods to be returned.
     * @param chainID The blockchain ID associated with the pending settlement methods to be returned.
     * @return A [List] of [OfferSettlementMethod]s
     */
    internal fun selectPendingOfferSettlementMethodByOfferIdAndChainID(
        offerID: String,
        chainID: String
    ): List<OfferSettlementMethod> {
        return dbQuery.selectPendingOfferSettlementMethodByOfferIdAndChainID(offerID, chainID).executeAsList().map {
            OfferSettlementMethod(
                id = it.id,
                chainID = it.chainID,
                settlementMethod = it.settlementMethod,
                privateData = it.privateData,
                privateDataInitializationVector = it.privateDataInitializationVector
            )
        }
    }

    /**
     * Returns key pairs with the specified interface ID.
     * @param interfaceId The interface ID of the key pairs to be returned.
     * @return A [List] of [KeyPair]s with interface IDs equal to [interfaceId]
     */
    internal fun selectKeyPairByInterfaceId(interfaceId: String): List<KeyPair> {
        return dbQuery.selectKeyPairByInterfaceId(interfaceId).executeAsList()
    }

    /**
     * Returns public keys with the specified interface ID.
     * @param interfaceId The interface ID of the public keys to be returned.
     * @return A [List] of [PublicKey]s with interface IDs equal to [interfaceId]
     */
    internal fun selectPublicKeyByInterfaceId(interfaceId: String): List<PublicKey> {
        return dbQuery.selectPublicKeyByInterfaceId(interfaceId).executeAsList()
    }

    /**
     * Returns [Swap](https://www.commuto.xyz/docs/technical-reference/core-tec-ref#swap)s with the specified swap ID.
     * @param id The ID of the Swaps to be returned.
     * @return A [List] of [Swap]s with swap IDs equal to [id].
     */
    internal fun selectSwapBySwapID(id: String): List<Swap> {
        return dbQuery.selectSwapBySwapID(id).executeAsList()
    }

    /**
     * Returns the user's [UserSettlementMethod]s with the specified ID.
     * @param id The ID of the settlement methods to be returned.
     * @return A [List] of [UserSettlementMethod]s
     */
    internal fun selectUserSettlementMethodByID(id: String): List<UserSettlementMethod> {
        return dbQuery.selectUserSettlementMethodByID(id).executeAsList()
    }

    /**
     * Inserts an [Offer] into the database.
     * @param offer The [Offer] to be inserted in the database.
     */
    internal fun insertOffer(offer: Offer) {
        dbQuery.insertOffer(
            id = offer.id,
            isCreated = offer.isCreated,
            isTaken = offer.isTaken,
            maker = offer.maker,
            interfaceId = offer.interfaceId,
            stablecoin = offer.stablecoin,
            amountLowerBound = offer.amountLowerBound,
            amountUpperBound = offer.amountUpperBound,
            securityDepositAmount = offer.securityDepositAmount,
            serviceFeeRate = offer.serviceFeeRate,
            onChainDirection = offer.onChainDirection,
            protocolVersion = offer.protocolVersion,
            chainID = offer.chainID,
            havePublicKey = offer.havePublicKey,
            isUserMaker = offer.isUserMaker,
            state = offer.state,
            cancelingOfferState = offer.cancelingOfferState,
            offerCancellationTransactionHash = offer.offerCancellationTransactionHash,
            offerCancellationTransactionCreationTime = offer.offerCancellationTransactionCreationTime,
            offerCancellationTransactionCreationBlockNumber = offer.offerCancellationTransactionCreationBlockNumber,
        )
    }

    /**
     * Inserts a [OfferSettlementMethod] into the database table of offers' settlement methods.
     * @param settlementMethod The [OfferSettlementMethod] to be inserted in the database.
     */
    internal fun insertOfferSettlementMethod(settlementMethod: OfferSettlementMethod) {
        dbQuery.insertOfferSettlementMethod(
            id = settlementMethod.id,
            chainID = settlementMethod.chainID,
            settlementMethod = settlementMethod.settlementMethod,
            privateData = settlementMethod.privateData,
            privateDataInitializationVector = settlementMethod.privateDataInitializationVector
        )
    }

    /**
     * Inserts a [OfferSettlementMethod] into the database table of offers' pending settlement methods.
     * @param pendingSettlementMethod The [OfferSettlementMethod] to be inserted into the database.
     */
    internal fun insertPendingOfferSettlementMethod(pendingSettlementMethod: OfferSettlementMethod) {
        dbQuery.insertPendingOfferSettlementMethod(
            id = pendingSettlementMethod.id,
            chainID = pendingSettlementMethod.chainID,
            settlementMethod = pendingSettlementMethod.settlementMethod,
            privateData = pendingSettlementMethod.privateData,
            privateDataInitializationVector = pendingSettlementMethod.privateDataInitializationVector
        )
    }

    /**
     * Inserts a [KeyPair] into the database.
     * @param keyPair The [KeyPair] to be inserted in the database.
     */
    internal fun insertKeyPair(keyPair: KeyPair) {
        dbQuery.insertKeyPair(
            interfaceId = keyPair.interfaceId,
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey,
        )
    }

    /**
     * Inserts a [PublicKey] into the database.
     * @param publicKey The [PublicKey] to be inserted in the database.
     */
    internal fun insertPublicKey(publicKey: PublicKey) {
        dbQuery.insertPublicKey(
            interfaceId = publicKey.interfaceId,
            publicKey = publicKey.publicKey,
        )
    }

    /**
     * Inserts a [Swap] into the database.
     * @param swap The [Swap] to be inserted ino the database.
     */
    internal fun insertSwap(swap: Swap) {
        dbQuery.insertSwap(
            id = swap.id,
            isCreated = swap.isCreated,
            requiresFill = swap.requiresFill,
            maker = swap.maker,
            makerInterfaceID = swap.makerInterfaceID,
            taker = swap.taker,
            takerInterfaceID = swap.takerInterfaceID,
            stablecoin = swap.stablecoin,
            amountLowerBound = swap.amountLowerBound,
            amountUpperBound = swap.amountUpperBound,
            securityDepositAmount = swap.securityDepositAmount,
            takenSwapAmount = swap.takenSwapAmount,
            serviceFeeAmount = swap.serviceFeeAmount,
            serviceFeeRate = swap.serviceFeeRate,
            onChainDirection = swap.onChainDirection,
            settlementMethod = swap.settlementMethod,
            makerPrivateData = swap.makerPrivateData,
            makerPrivateDataInitializationVector = swap.makerPrivateDataInitializationVector,
            takerPrivateData = swap.takerPrivateData,
            takerPrivateDataInitializationVector = swap.takerPrivateDataInitializationVector,
            protocolVersion = swap.protocolVersion,
            isPaymentSent = swap.isPaymentSent,
            isPaymentReceived = swap.isPaymentReceived,
            hasBuyerClosed = swap.hasBuyerClosed,
            hasSellerClosed = swap.hasSellerClosed,
            disputeRaiser = swap.disputeRaiser,
            chainID = swap.chainID,
            state = swap.state,
            role = swap.role
        )
    }

    /**
     * Inserts a [UserSettlementMethod] into the database table of the user's settlement methods.
     * @param settlementMethod The [UserSettlementMethod] to be inserted in the database.
     */
    internal fun insertUserSettlementMethod(settlementMethod: UserSettlementMethod) {
        dbQuery.insertUserSettlementMethod(
            settlementMethodID = settlementMethod.settlementMethodID,
            settlementMethod = settlementMethod.settlementMethod,
            privateData = settlementMethod.privateData,
            privateDataInitializationVector = settlementMethod.privateDataInitializationVector
        )
    }

    /**
     * Updates the [Offer.havePublicKey] property of the [Offer] with the specified [offerID] and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which [Offer] to be updated exists.
     * @param havePublicKey The new value of the [Offer.havePublicKey] property.
     */
    internal fun updateOfferHavePublicKey(offerID: String, chainID: String, havePublicKey: Long) {
        dbQuery.updateOfferHavePublicKeyByOfferIDAndChainID(
            havePublicKey = havePublicKey,
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Offer.state] property of the [Offer] with the specified [offerID] and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which the [Offer] to be updated exists.
     * @param state The new value of the [Offer.state] property.
     */
    internal fun updateOfferState(offerID: String, chainID: String, state: String) {
        dbQuery.updateOfferStateByOfferIDAndChainID(
            state = state,
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Offer.cancelingOfferState] property of the [Offer] with the specified [offerID] and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which the [Offer] to be updated exists.
     * @param state The new value of the [Offer.cancelingOfferState] property.
     */
    internal fun updateCancelingOfferState(offerID: String, chainID: String, state: String) {
        dbQuery.updateCancelingOfferStateByOfferIDAndChainID(
            cancelingOfferState = state,
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Offer.offerCancellationTransactionHash], [Offer.offerCancellationTransactionCreationTime] and
     * [Offer.offerCancellationTransactionCreationBlockNumber] properties of the [Offer] with the specified [offerID]
     * and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which the [Offer] to be updated exists.
     * @param transactionHash The new value of the [Offer.offerCancellationTransactionHash] property.
     * @param creationTime The new value of the [Offer.offerCancellationTransactionCreationTime] property.
     * @param blockNumber The new value of the [Offer.offerCancellationTransactionCreationBlockNumber] property.
     */
    internal fun updateOfferCancellationTransactionHash(
        offerID: String,
        chainID: String,
        transactionHash: String?,
        creationTime: String?,
        blockNumber: Long?
    ) {
        dbQuery.updateOfferCancellationDataByOfferIDAndChainID(
            offerCancellationTransactionHash = transactionHash,
            offerCancellationTransactionCreationTime = creationTime,
            offerCancellationTransactionCreationBlockNumber = blockNumber,
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Swap.requiresFill] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param requiresFill The new value of the [Swap.requiresFill] property.
     */
    internal fun updateSwapRequiresFill(swapID: String, chainID: String, requiresFill: Long) {
        dbQuery.updateSwapRequiresFillBySwapIDAndChainID(
            requiresFill = requiresFill,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [Swap.makerPrivateData] and [Swap.makerPrivateDataInitializationVector] properties of the [Swap] with
     * the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param encryptedData The new value of the [Swap.makerPrivateData] property.
     * @param initializationVector The new value of the [Swap.makerPrivateDataInitializationVector] property.
     */
    internal fun updateSwapMakerPrivateSettlementMethodData(
        swapID: String,
        chainID: String,
        encryptedData: String?,
        initializationVector: String?
    ) {
        dbQuery.updateSwapMakerPrivateSettlementMethodData(
            makerPrivateData = encryptedData,
            makerPrivateDataInitializationVector = initializationVector,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [Swap.takerPrivateData] and [Swap.takerPrivateDataInitializationVector] properties of the [Swap] with
     * the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param encryptedData The new value of the [Swap.takerPrivateData] property.
     * @param initializationVector The new value of the [Swap.takerPrivateDataInitializationVector] property.
     */
    internal fun updateSwapTakerPrivateSettlementMethodData(
        swapID: String,
        chainID: String,
        encryptedData: String?,
        initializationVector: String?
    ) {
        dbQuery.updateSwapTakerPrivateSettlementMethodData(
            takerPrivateData = encryptedData,
            takerPrivateDataInitializationVector = initializationVector,
            id = swapID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Swap.isPaymentSent] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param isPaymentSent The new value of the [Swap.isPaymentSent] property.
     */
    internal fun updateSwapIsPaymentSent(swapID: String, chainID: String, isPaymentSent: Long) {
        dbQuery.updateSwapIsPaymentSentBySwapIDAndChainID(
            isPaymentSent = isPaymentSent,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [Swap.isPaymentReceived] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param isPaymentReceived The new value of the [Swap.isPaymentReceived] property.
     */
    internal fun updateSwapIsPaymentReceived(swapID: String, chainID: String, isPaymentReceived: Long) {
        dbQuery.updateSwapIsPaymentReceivedBySwapIDAndChainID(
            isPaymentReceived = isPaymentReceived,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [Swap.hasBuyerClosed] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param hasBuyerClosed The new value of the [Swap.hasBuyerClosed] property.
     */
    internal fun updateSwapHasBuyerClosed(swapID: String, chainID: String, hasBuyerClosed: Long) {
        dbQuery.updateSwapHasBuyerClosedBySwapIDAndChainID(
            hasBuyerClosed = hasBuyerClosed,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [Swap.hasSellerClosed] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param hasSellerClosed The new value of the [Swap.hasSellerClosed] property.
     */
    internal fun updateSwapHasSellerClosed(swapID: String, chainID: String, hasSellerClosed: Long) {
        dbQuery.updateSwapHasSellerClosedBySwapIDAndChainID(
            hasSellerClosed = hasSellerClosed,
            id = swapID,
            chainID = chainID,
        )
    }

    /**
     * Updates the [UserSettlementMethod.privateData] and [UserSettlementMethod.privateDataInitializationVector]
     * properties of the [UserSettlementMethod] with the specified [id].
     * @param id The ID of the [UserSettlementMethod] to be updated.
     * @param privateData The new value of the [UserSettlementMethod.privateData] property.
     * @param privateDataInitializationVector The new value of the
     * [UserSettlementMethod.privateDataInitializationVector] property.
     */
    internal fun updateUserSettlementMethod(
        id: String,
        privateData: String?,
        privateDataInitializationVector: String?
    ) {
        dbQuery.updateUserSettlementMethodByID(
            privateData = privateData,
            privateDataInitializationVector = privateDataInitializationVector,
            settlementMethodID = id
        )
    }

    /**
     * Deletes all [Offer]s with the specified offer ID and chain ID from the database.
     * @param offerID The offer ID of the [Offer]s to be deleted.
     * @param chainID The blockchain ID of the [Offer]s to be deleted.
     */
    internal fun deleteOffer(offerID: String, chainID: String) {
        dbQuery.deleteOfferByOfferIdAndChainID(
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Swap.state] property of the [Swap] with the specified [swapID] and [chainID].
     * @param swapID The ID of the [Swap] to be updated.
     * @param chainID The ID of the blockchain on which the [Swap] to be updated exists.
     * @param state The new value of the [Swap.state] property.
     */
    internal fun updateSwapState(swapID: String, chainID: String, state: String) {
        dbQuery.updateSwapStateBySwapIDAndChainID(
            state = state,
            id = swapID,
            chainID = chainID
        )
    }

    /**
     * Deletes all [OfferSettlementMethod]s with the specified offer ID and specified blockchain ID from the database table
     * of offers' current settlement methods.
     * @param offerID The offer ID of the [OfferSettlementMethod]s to be deleted.
     * @param chainID The blockchain ID of the [OfferSettlementMethod]s to be deleted.
     */
    internal fun deleteOfferSettlementMethods(offerID: String, chainID: String) {
        dbQuery.deleteOfferSettlementMethodByOfferIdAndChainID(
            id = offerID,
            chainID = chainID
        )
    }

    /**
     * Deletes all [OfferSettlementMethod]s with the specified offer ID and specified blockchain ID from the database table
     * of offers' pending settlement methods.
     * @param offerID The offer ID of the [OfferSettlementMethod]s to be deleted.
     * @param chainID The blockchain ID of the [OfferSettlementMethod]s to be deleted.
     */
    internal fun deletePendingOfferSettlementMethods(offerID: String, chainID: String) {
        dbQuery.deletePendingOfferSettlementMethodByOfferIdAndChainID(
            id = offerID,
            chainID = chainID,
        )
    }

    /**
     * Deletes all [Swap]s with the specified swap ID and chain ID from the database.
     * @param swapID The ID of the [Swap]s to be deleted.
     * @param chainID The blockchain ID of the [Swap]s to be deleted.
     */
    internal fun deleteSwap(swapID: String, chainID: String) {
        dbQuery.deleteSwapBySwapIDAndChainID(
            id = swapID,
            chainID = chainID
        )
    }

    /**
     * Deletes all [UserSettlementMethod]s with the specified ID from the database.
     * @param id The ID of the [UserSettlementMethod]s to be deleted.
     */
    internal fun deleteUserSettlementMethod(id: String) {
        dbQuery.deleteUserSettlementMethodByID(
            settlementMethodID = id
        )
    }

}