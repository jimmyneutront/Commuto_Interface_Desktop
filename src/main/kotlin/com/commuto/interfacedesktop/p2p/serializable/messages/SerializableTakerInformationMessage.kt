package com.commuto.interfacedesktop.p2p.serializable.messages

import kotlinx.serialization.Serializable

/**
 * A [Serializable] class representing a
 * [Taker Information Message](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 */
@Serializable
class SerializableTakerInformationMessage(
    val sender: String,
    val recipient: String,
    val encryptedKey: String,
    val encryptedIV: String,
    val payload: String,
    val signature: String
)