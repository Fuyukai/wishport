/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.linux.RLIMIT_NOFILE
import platform.linux.getrlimit
import platform.linux.rlimit
import platform.linux.setrlimit
import platform.posix.SIGPIPE
import platform.posix.SIG_IGN
import platform.posix.signal

internal actual fun setupPlatform() = memScoped {
    // 1) mask off sigpipe, in case someone does some stupid read() on a shutdown socket
    signal(SIGPIPE, SIG_IGN)

    // 2) bump up fd limit
    val limit = alloc<rlimit>()
    assert(getrlimit(RLIMIT_NOFILE, limit.ptr) == 0) { "getrlimit(RLIMIT_NOFILE, *out) failed?" }
    limit.rlim_cur = limit.rlim_max
    assert(setrlimit(RLIMIT_NOFILE, limit.ptr) == 0) { "setrlimit() failed?" }
}
