/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec
import tf.veriny.wishport.SecureRandom

@ThreadLocal
private val SHARED_TIMESPEC = nativeHeap.alloc<timespec>()

private val OFFSET = SecureRandom.nextLong(0x00FF_FFFF_FFFF_FFFF)

// On modern versions of linux, this should avoid system calls as clock_gettime is vDSO'd.
public actual fun getMonotonicTime(): Long {
    val res = clock_gettime(CLOCK_REALTIME, SHARED_TIMESPEC.ptr)
    if (res != 0) throw Throwable("clock_gettime failed?")

    var time = (SHARED_TIMESPEC.tv_sec * 1_000_000_000) + SHARED_TIMESPEC.tv_nsec
    time += OFFSET
    return time
}
