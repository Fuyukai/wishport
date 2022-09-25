/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.clock_nanosleep
import platform.posix.timespec
import tf.veriny.wishport.SecureRandom
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.NS_PER_SEC

@ThreadLocal
private val SHARED_TIMESPEC = nativeHeap.alloc<timespec>()

private val OFFSET = SecureRandom.nextLong(0x0000_0000_FFFF_FFFF)

// On modern versions of linux, this should avoid system calls as clock_gettime is vDSO'd.
@LowLevelApi
public actual fun getMonotonicTime(): Long {
    val res = clock_gettime(CLOCK_MONOTONIC, SHARED_TIMESPEC.ptr)
    if (res != 0) throw Throwable("clock_gettime failed?")

    var time = (SHARED_TIMESPEC.tv_sec * NS_PER_SEC) + SHARED_TIMESPEC.tv_nsec
    time += OFFSET
    return time
}

@LowLevelApi
public actual fun nanosleep(ns: Long): Unit = memScoped {
    val ts = alloc<timespec>()
    ts.tv_sec = (ns / NS_PER_SEC)
    ts.tv_nsec = (ns.rem(1_000_000_000))

    // just assume this can never fail
    // also, use clock_nanosleep as posix says nanosleep is relative to CLOCK_REALTIME but we
    // want CLOCK_MONOTONIC. linux uses monotonic by default but i prefer being explicit
    val res = clock_nanosleep(CLOCK_MONOTONIC, 0, ts.ptr, null)
    if (res != 0) throw Throwable("clock nanosleep failed???")
}
