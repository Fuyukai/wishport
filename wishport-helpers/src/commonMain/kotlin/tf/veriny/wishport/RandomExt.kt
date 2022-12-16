/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("PrivatePropertyName")

package tf.veriny.wishport

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import kotlin.random.Random

private val ALLOWED_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')

/**
 * Generates a random alphanumeric string of the specified [length].
 */
public fun Random.randomString(length: Int): String {
    val builder = StringBuilder(length)
    repeat(length) {
        val idx = nextInt(ALLOWED_CHARS.size)
        builder.append(ALLOWED_CHARS[idx])
    }
    return builder.toString()
}

/**
 * Generates a random alphanumeric [ByteString] of the speecified [length].
 */
@OptIn(Unsafe::class)
public fun Random.randomByteString(length: Int): ByteString {
    val buf = ByteArray(length)
    for (i in 0 until length) {
        val idx = nextInt(ALLOWED_CHARS.size)
        // these are ascii so its fine.
        buf[i] = ALLOWED_CHARS[idx].code.toByte()
    }

    return ByteString.uncopied(buf)
}