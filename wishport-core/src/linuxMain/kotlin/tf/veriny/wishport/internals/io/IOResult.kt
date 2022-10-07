/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

import platform.linux.EPOLLERR
import platform.linux.EPOLLIN
import platform.linux.EPOLLOUT

// see the expect definitions

public actual sealed interface IOResult

public actual interface IOHandle {
    public val actualFd: Int
}

public class Fd(public override val actualFd: Int) : IOResult, IOHandle

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