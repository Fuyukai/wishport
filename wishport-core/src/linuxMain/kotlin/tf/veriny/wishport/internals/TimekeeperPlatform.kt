/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import kotlinx.cinterop.*
import platform.posix.*

// K/N uses a really ancient version of glibc (2.19, 8 years old), so we can't use getrandom() here.
// Instead we just open /dev/urandom and read 6 bytes from it, and construct a long
// through bitshifting those upwards.
// A better solution would be to use the getrandom() syscall directly, but I don't care enough.
// This will be refactored in the future when I write a real urandom shim that impls Kotlin
// random.
// Also I don't care about error handling here at all. None of these calls should ever fail.

@ThreadLocal
private val SHARED_TIMESPEC = nativeHeap.alloc<timespec>()

private val OFFSET = run {
    val fd = open("/dev/urandom", O_CLOEXEC)
    try {
        if (fd < 0) {
            throw Throwable("failed to open /dev/urandom")
        }

        val buf = ByteArray(6)
        val read = buf.usePinned {
            read(fd, it.addressOf(0), 6)
        }
        if (read < 0) {
            throw Throwable("failed to read /dev/urandom")
        }

        var res = buf[0].toLong().shl(5 * 8)
        res = res.or(buf[1].toLong().shl(4 * 8))
        res = res.or(buf[2].toLong().shl(3 * 8))
        res = res.or(buf[3].toLong().shl(2 * 8))
        res = res.or(buf[4].toLong().shl(8))
        res = res.or(buf[5].toLong())
        res
    } finally {
        close(fd)
    }
}

// On modern versions of linux, this should avoid system calls as clock_gettime is vDSO'd.
public actual fun getMonotonicTime(): Long {
    val res = clock_gettime(CLOCK_REALTIME, SHARED_TIMESPEC.ptr)
    if (res != 0) throw Throwable("clock_gettime failed?")

    return (SHARED_TIMESPEC.tv_sec * 1_000_000_000) + SHARED_TIMESPEC.tv_nsec
}
