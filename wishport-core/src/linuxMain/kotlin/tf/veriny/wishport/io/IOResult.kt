/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import platform.linux.EPOLLERR
import platform.linux.EPOLLIN
import platform.linux.EPOLLOUT
import tf.veriny.wishport.collections.FastArrayList

// marker interfaces
public actual interface IOHandle {
    public val actualFd: Int
}
public actual sealed interface IOResult

private val PREALLOCATED_FDS = (0..256).mapTo(FastArrayList(256)) { Fd(it) }

public class Fd(override val actualFd: Int) : IOHandle, IOResult {
    public companion object {
        public fun get(actualFd: Int): Fd {
            return if (actualFd <= 256) PREALLOCATED_FDS[actualFd]
            else Fd(actualFd)
        }
    }
}

public actual typealias SocketHandle = Fd
public actual typealias DirectoryHandle = Fd
public actual typealias RawFileHandle = Fd

/**
 * Converts a [PollResult] into a set of [Poll] flags.
 */
public fun intoFlags(result: Int): Set<Poll> {
    val flags = mutableSetOf<Poll>()

    if (result.and(EPOLLIN) != 0) flags.add(Poll.POLL_READ)
    if (result.and(EPOLLOUT) != 0) flags.add(Poll.POLL_WRITE)
    if (result.and(EPOLLERR) != 0) flags.add(Poll.POLL_ERROR)

    return flags
}
