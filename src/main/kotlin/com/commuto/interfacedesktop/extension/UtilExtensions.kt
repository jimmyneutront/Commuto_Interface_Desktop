package com.commuto.interfacedesktop.extension

import java.nio.ByteBuffer
import java.util.*

/**
 * Returns the [UUID] as a [ByteArray]
 */
fun UUID.asByteArray(): ByteArray {
    return ByteBuffer.wrap(ByteArray(16)).also {
        it.putLong(this.mostSignificantBits).putLong(this.leastSignificantBits)
    }.array()
}