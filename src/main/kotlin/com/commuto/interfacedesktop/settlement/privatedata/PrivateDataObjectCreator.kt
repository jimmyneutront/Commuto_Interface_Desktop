package com.commuto.interfacedesktop.settlement.privatedata

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Attempts to create an object implementing [PrivateData] by deserializing [privateData] as JSON, returning the result,
 * or `null` if [privateData] cannot be deserialized into an object implementing [PrivateData].
 *
 * @param privateData The [String] from which this attempts to create an object implementing [PrivateData].
 *
 * @return An object implementing [PrivateData] derived by deserializing [privateData], or `null` if deserialization
 * fails.
 */
fun createPrivateDataObject(privateData: String): PrivateData? {
    var privateDataObject: PrivateData? = try {
        Json.decodeFromString<PrivateSEPAData>(string = privateData)
    } catch (e: Exception) {
        null
    }
    if (privateDataObject != null) {
        return privateDataObject
    }
    privateDataObject = try {
        Json.decodeFromString<PrivateSWIFTData>(string = privateData)
    } catch (e: Exception) {
        null
    }
    if (privateDataObject != null) {
        return privateDataObject
    }
    return null
}