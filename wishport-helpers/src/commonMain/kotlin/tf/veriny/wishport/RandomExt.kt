/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlin.random.Random

private val ALLOWED_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')

/**
 * Generates a random alphanumeric string of the specified [length].
 */
public fun Random.randomString(length: Int): String {
    val builder = StringBuilder(length)
    for (i in 0 until length) {
        val idx = nextInt(ALLOWED_CHARS.size)
        builder.append(ALLOWED_CHARS[idx])
    }
    return builder.toString()
}
