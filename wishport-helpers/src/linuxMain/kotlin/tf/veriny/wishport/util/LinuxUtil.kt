/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.util

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.strerror_r
import tf.veriny.wishport.EINVAL
import tf.veriny.wishport.ERANGE

/**
 * Converts an integer [errno] into a human-readable string.
 */
public fun kstrerror(errno: Int): String = memScoped<String> {
    var size = 64

    while (true) {
        val buf = ByteArray(size)

        val res = buf.usePinned {
            strerror_r(errno, it.addressOf(0), buf.size.toULong())
        }

        return if (res > 0) {
            if (res == EINVAL) "Unknown"
            else if (res == ERANGE) {
                size *= 2
                continue
            } else throw Throwable("strerror_r returned $res, which isn't allowed!")
        } else {
            buf.toKString()
        }
    }

    // type inference bug?
    throw Throwable("unreachable")
}
