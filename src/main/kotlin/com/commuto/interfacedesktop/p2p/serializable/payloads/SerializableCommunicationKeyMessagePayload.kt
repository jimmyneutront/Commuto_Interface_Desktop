package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Serializable] class representing the payload of a
 * [Communication Key Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 */
@Serializable
data class SerializableCommunicationKeyMessagePayload(
    val msgType: String,
    val swapId: String,
    val chainID: String,
    val key: String,
)
