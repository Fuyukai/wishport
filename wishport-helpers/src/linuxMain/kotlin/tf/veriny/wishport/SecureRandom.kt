/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import external.getrandom.wp_getrandom
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import platform.posix.EINTR
import platform.posix.posix_errno
import platform.posix.size_t

// Safety: There's 5 possible errors for this function.
// 1) ``EAGAIN``, which can't happen as we don't pass GRND_NONBLOCK.
// 2) ``EFAULT``, which would be a Kotlin-level bug.
// 3) ``EINTR``, which we handle ourselves.
// 4) ``EINVAL``, which can't happen as we always pass 0 for flags.
// 5) ``ENOSYS``, which only happens on really old (i.e. <3.17) kernels. We're on 6.1 now, so this
//    is a non-worry.
// Thus, you can conclude this function will *never* fail.

internal actual fun getrandom(ptr: CPointer<ByteVar>, size: size_t) {
    while (true) {
        val result = wp_getrandom(
            ptr, size,
            0
        )

        if (result == -1L) {
            if (posix_errno() == EINTR) continue
            else throw Throwable("getrandom() failed")
        } else break
    }
}
