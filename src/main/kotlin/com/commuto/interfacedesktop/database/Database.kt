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
        dbQuery.createPublicKeyTable()
        dbQuery.createKeyPairTable()
    }

    /**
     * Deletes everything from every database table.
     */
    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.removeAllOffers()
            dbQuery.removeAllSettlementMethods()
            dbQuery.removeAllKeyPairs()
            dbQuery.removeAllPublicKeys()
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
     * Returns [SettlementMethod]s with the specified offer ID and blockchain ID.
     * @param offerID The offer ID associated with the settlement methods to be returned.
     * @param chainID The blockchain ID associated with the settlement methods to be returned.
     * @return A [List] of [SettlementMethod]s
     */
    internal fun selectSettlementMethodByOfferIdAndChainID(offerID: String, chainID: String): List<SettlementMethod> {
        return dbQuery.selectSettlementMethodByOfferIdAndChainID(offerID, chainID).executeAsList()
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
     * Inserts an [Offer] into the database.
     * @param offer The [Offer] to be inserted in the database.
     */
    internal fun insertOffer(offer: Offer) {
        dbQuery.insertOffer(
            offerId = offer.offerId,
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
            state = offer.state
        )
    }

    /**
     * Inserts a [SettlementMethod] into the database.
     * @param settlementMethod The [SettlementMethod] to be inserted in the database.
     */
    internal fun insertSettlementMethod(settlementMethod: SettlementMethod) {
        dbQuery.insertSettlementMethod(
            offerId = settlementMethod.offerId,
            chainID = settlementMethod.chainID,
            settlementMethod = settlementMethod.settlementMethod,
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
     * Updates the [Offer.havePublicKey] property of the [Offer] with the specified [offerID] and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which [Offer] to be updated exists.
     * @param havePublicKey The new value of the [Offer.havePublicKey] property.
     */
    internal fun updateOfferHavePublicKey(offerID: String, chainID: String, havePublicKey: Long) {
        dbQuery.updateOfferHavePublicKeyByOfferIDAndChainID(
            havePublicKey = havePublicKey,
            offerId = offerID,
            chainID = chainID
        )
    }

    /**
     * Updates the [Offer.state] property of the [Offer] with the specified [offerID] and [chainID].
     * @param offerID The ID of the [Offer] to be updated.
     * @param chainID The ID of the blockchain on which [Offer] to be updated exists.
     * @param state The new value of the [Offer.state] property.
     */
    internal fun updateOfferState(offerID: String, chainID: String, state: String) {
        dbQuery.updateOfferStateByOfferIDAndChainID(
            state = state,
            offerId = offerID,
            chainID = chainID
        )
    }

    /**
     * Deletes all [Offer]s with the specified offer ID and chain ID from the database.
     * @param offerID The offer ID of the [Offer]s to be deleted.
     * @param chainID The blockchain ID of the [Offer]s to be deleted.
     */
    internal fun deleteOffer(offerID: String, chainID: String) {
        dbQuery.deleteOfferByOfferIdAndChainID(
            offerId = offerID,
            chainID = chainID
        )
    }

    /**
     * Deletes all [SettlementMethod]s with the specified offer ID and specified blockchain ID from the database.
     * @param offerID The offer ID of the [SettlementMethod]s to be deleted.
     * @param chainID The blockchain ID of the [SettlementMethod]s to be deleted.
     */
    internal fun deleteSettlementMethods(offerID: String, chainID: String) {
        dbQuery.deleteSettlementMethodByOfferIdAndChainID(
            offerId = offerID,
            chainID = chainID
        )
    }

}