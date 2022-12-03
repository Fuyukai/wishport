/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.util

import kotlinx.cinterop.*
import platform.extra.wp_openat
import platform.posix.*
import tf.veriny.wishport.flags
import tf.veriny.wishport.getEnvironmentVariable

// exception classes
public class OSError(
    public val errno: Int, message: String = "Error:",
) : Exception("$message: ${kstrerror(errno)}")

public class ProcNotFoundError : Exception("Could not find a valid /proc filesystem")

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
    throw OSError(posix_errno(), message)
}

// this stays open
private var procFd = -1

/**
 * Gets the file descriptor referring to the /proc filesystem. This can be used for subsequent
 * calls to openat() to get information relating to the currently running system.
 */
public fun getProcFileDescriptor(): Int {
    if (procFd > 0) return procFd

    // 1) try opening /proc directly
    val procLocation = getEnvironmentVariable("WISHPORT_PROC_LOCATION") ?: "/proc"
    val fd = open(procLocation, flags(O_RDONLY, O_DIRECTORY, O_CLOEXEC))
    if (fd < 0) {
        val errno = posix_errno()
        when (errno) {
            ENOENT, ENOTDIR, EPERM -> throw ProcNotFoundError()
            else -> perror("failed to open /proc")
        }
    }

    procFd = fd
    return procFd
}

public val kernelVersion: KernelVersion by lazy {
    val dirfd = getProcFileDescriptor()
    val fd = wp_openat(dirfd, "sys/kernel/osrelease".cstr, O_RDONLY, 0)
    if (fd < 0) {
        val errno = posix_errno()
        if (errno == ENOENT) throw ProcNotFoundError()
        else perror("failed to open /proc/sys/kernel/osrelease")
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
