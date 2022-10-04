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

public fun kstrerror(errno: Int): String = memScoped {
    // this is large, but should always be sufficient
    // if any strerror is too big, i'll update it
    val buf = ByteArray(1024)
    val res = buf.usePinned {
        strerror_r(errno, it.addressOf(0), buf.size.toULong())
    }

    // le
    return buf.toKString()
}
