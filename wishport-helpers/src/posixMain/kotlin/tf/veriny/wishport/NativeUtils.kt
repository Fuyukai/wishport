/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlinx.cinterop.*
import platform.extra.strlen
import platform.extra.strnlen
import platform.posix.memcpy
import tf.veriny.wishport.annotations.Unsafe

/**
 * Reads out a Kotlin [ByteArray] from a [CArrayPointer].
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Unsafe
public fun CArrayPointer<ByteVar>.readZeroTerminated(): ByteArray {
    val length = strlen(this)
    require(length < UInt.MAX_VALUE) { "Size $length is too big" }

    val buf = ByteArray(length.toInt())
    buf.usePinned {
        memcpy(it.addressOf(0), this, buf.size.toULong())
    }
    return buf
}

/**
 * Reads out a Kotlin [ByteArray] from a [CArrayPointer], with maximum size [maxSize] to avoid
 * buffer overflows.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Unsafe
public fun CArrayPointer<ByteVar>.readZeroTerminated(maxSize: Int): ByteArray {
    val length = strnlen(this, maxSize.toULong())
    require(length < UInt.MAX_VALUE) { "Size $length is too big" }

    val buf = ByteArray(length.toInt())
    buf.usePinned {
        memcpy(it.addressOf(0), this, buf.size.toULong())
    }
    return buf
}

/**
 * Reads out a Kotlin [String] from a [CArrayPointer], but faster than the native method.
 */
@Unsafe
public fun CPointer<ByteVar>.toKStringUtf8Fast(): String {
    val ba = readZeroTerminated()
    return ba.decodeToString()
}

/**
 * Reads bytes from a [COpaquePointer] using memcpy() instead of the naiive Kotlin
 * byte-by-byte copy.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Unsafe
public fun COpaquePointer.readBytesFast(count: Int): ByteArray {
    val buf = ByteArray(count)
    buf.usePinned {
        memcpy(it.addressOf(0), this, buf.size.toULong())
    }
    return buf
}

/**
 * Overwrites the memory pointed to with a [ByteArray].
 */
@Unsafe
public fun CArrayPointer<ByteVar>.unsafeClobber(other: ByteArray) {
    for (idx in other.indices) {
        this[idx] = other[idx]
    }
}

/**
 * Creates a new [ByteArray] that is null-terminated, for passing to C functions.
 */
public fun ByteArray.toNullTerminated(): ByteArray {
    if (this.last() == (0).toByte()) {
        return this
    }
    return this.copyInto(ByteArray(size + 1) { 0 })
}
