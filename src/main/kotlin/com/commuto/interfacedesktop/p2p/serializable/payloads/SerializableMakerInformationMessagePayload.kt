package com.commuto.interfacedesktop.p2p.serializable.payloads

import kotlinx.serialization.Serializable

/**
 * A [Serializable] class representing the payload of a
 * [Maker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 */
@Serializable
class SerializableMakerInformationMessagePayload(
    val msgType: String,
    val swapId: String,
    var paymentDetails: String
)