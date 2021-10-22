package com.commuto.interfacedesktop.db

import com.commuto.interfacedesktop.db.CommutoInterfaceDB

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CommutoInterfaceDB(databaseDriverFactory.createDriver())
    private val dbQuery = database.commutoInterfaceDBQueries

    internal fun createTables() {
        dbQuery.createPublicKeyTable()
        dbQuery.createKeyPairTable()
    }

    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.removeAllKeyPairs()
            dbQuery.removeAllPublicKeys()
        }
    }

    internal fun selectKeyPairByInterfaceId(interfaceId: String): List<KeyPair> {
        return dbQuery.selectKeyPairByInterfaceId(interfaceId).executeAsList()
    }

    internal fun selectPublicKeyByInterfaceId(interfaceId: String): List<PublicKey> {
        return dbQuery.selectPublicKeyByInterfaceId(interfaceId).executeAsList()
    }

    internal fun insertKeyPair(keyPair: KeyPair) {
        dbQuery.insertKeyPair(
            interfaceId = keyPair.interfaceId,
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey,
        )
    }

    internal fun insertPublicKey(publicKey: PublicKey) {
        dbQuery.insertPublicKey(
            interfaceId = publicKey.interfaceId,
            publicKey = publicKey.publicKey,
        )
    }
}