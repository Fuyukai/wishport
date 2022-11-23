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
import platform.posix.*

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

public data class KernelVersion(
    public val major: Int,
    public val minor: Int,
    public val patch: Int,
)

private inline fun perror(message: String): Nothing {
    val error = kstrerror(posix_errno())
    throw UnsupportedOperationException("$message: $error")
}

public val kernelVersion: KernelVersion by lazy {
    val fd = open("/proc/sys/kernel/osrelease", O_RDONLY)
    if (fd < 0) {
        perror("failed to open /proc/sys/kernel/osrelease")
    }

    try {
        val buf = ByteArray(32)
        buf.usePinned {
            val res = read(fd, it.addressOf(0), 32)
            if (res < 0) {
                perror("failed to read() open file somehow")
            }
        }
        val release = buf.toKString()

        val items = if ('-' in release) {
            val (before, _) = release.split('-', limit = 2)
            before.split(".").map { it.toInt() }
        } else {
            release.split(".").map { it.toInt() }
        }
        KernelVersion(items[0], items[1], items[2])
    } finally {
        close(fd)
    }
}
