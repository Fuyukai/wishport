/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.util

import kotlinx.cinterop.*
import platform.posix.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.toKStringUtf8Fast

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

/**
 * Wraps the result of a uname(2) call.
 */
public data class KernelInfo(
    /** The node name for this machine, aka the hostname. */
    public val nodeName: String,
    /** The kernel release string, e.g. "6.0.7-arch1-1". */
    public val release: String,
    public val version: String,
    public val machine: String,
) {
    private val ver by lazy {
        if ('-' in release) {
            val (before, after) = release.split('-', limit = 2)
            before.split(".").map { it.toInt() }
        } else {
            release.split(".").map { it.toInt() }
        }
    }

    public val major: Int get() = ver[0]
    public val minor: Int get() = ver[1]
    public val patch: Int get() = ver[2]
}

private lateinit var info: KernelInfo

/**
 * Gets the current Linux kernel version.
 */
@OptIn(Unsafe::class)
public fun getKernelInfo(): KernelInfo {
    if (::info.isInitialized) return info

    // leak it into the ether as for some reason K/N can't free this properly? or I have
    // a leak somewhere else that I don't know about and it's conflicting somehow.
    val buf = nativeHeap.alloc<utsname>()
    val res = uname(buf.ptr)
    if (res != 0) error("what the fuck?")

    info = KernelInfo(
        buf.nodename.toKStringUtf8Fast(),
        buf.release.toKStringUtf8Fast(),
        buf.version.toKStringUtf8Fast(),
        buf.machine.toKStringUtf8Fast()
    )

    return info
}