/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlinx.cinterop.*
import platform.posix.size_t
import kotlin.experimental.and
import kotlin.random.Random

/**
 * Gets random bytes and fills them into the specified [ptr].
 */
public expect fun getrandom(ptr: CPointer<ByteVar>, size: size_t)

/**
 * A secure random number generator based on a platform call. On Linux, this uses the getrandom()
 * system call (on AMD64 directly, on arm64 via glibc); on Windows this uses BCryptGenRandom.
 *
 * Please note that due to the contact of ``kotlin.random.Random``, these methods all throw errors.
 * If you want an error-free return, use ``read(size)`` directly, which will never fail.
 */
public actual object SecureRandom : Random() {
    private fun getrandom(buf: ByteArray) = memScoped {
        buf.usePinned {
            getrandom(it.addressOf(0), buf.size.toULong())
        }
    }

    /**
     * Reads [size] random bytes.
     */
    public fun read(size: Int): ByteArray {
        val buf = ByteArray(size)
        getrandom(buf)
        return buf
    }

    // required, inefficient override
    override fun nextBits(bitCount: Int): Int {
        val numBytes = (bitCount + 7) / 8
        var next = 0
        val buf = ByteArray(numBytes)
        getrandom(buf)

        for (i in 0 until numBytes) {
            next = (next shl 8) + (buf[i] and ((0xFF).toByte()))
        }

        return next ushr (numBytes * 8 - bitCount)
    }

    // more efficient overrides
    override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray {
        check(fromIndex > 0) { "cannot have negative from-indexes" }
        check(fromIndex < array.size) { "first index out of range" }
        check(toIndex > fromIndex) { "second index cannot be lower than first index" }
        check(toIndex < array.size) { "second index out of range" }

        array.usePinned {
            getrandom(it.addressOf(fromIndex), toIndex.toULong())
        }

        return array
    }

    override fun nextBytes(array: ByteArray): ByteArray {
        getrandom(array)
        return array
    }
}
