package com.commuto.interfacedesktop.p2p.serializable.messages

import kotlinx.serialization.Serializable

/**
 * A [Serializable] class representing an encrypted message, as described in the
 * [Commuto Interface Specification](https://github.com/jimmyneutront/commuto-whitepaper/blob/main/commuto-interface-specification.txt)
 */
@Serializable
class SerializableEncryptedMessage(
    val sender: String,
    val recipient: String,
    val encryptedKey: String,
    val encryptedIV: String,
    val payload: String,
    val signature: String
)