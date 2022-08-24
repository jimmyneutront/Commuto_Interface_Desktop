package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Serializable] class representing the payload of a
 * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 */
@Serializable
class SerializableTakerInformationMessagePayload(
    val msgType: String,
    val pubKey: String,
    val swapId: String,
    var paymentDetails: String
)