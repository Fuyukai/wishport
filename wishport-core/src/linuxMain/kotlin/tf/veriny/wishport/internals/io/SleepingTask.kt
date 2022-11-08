/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.io.IOResult

/**
 * Wraps the state of a task currently waiting for an io_uring event.
 */
internal data class SleepingTask @OptIn(LowLevelApi::class) constructor(
    val task: Task,
    val id: ULong,
    val why: SleepingWhy
) {
    var completed: Boolean = false

    // set by the cqe poller
    lateinit var wakeupData: CancellableResourceResult<IOResult>
}

/** Enumeration of what the task is sleeping on. */
internal enum class SleepingWhy {
    CLOSE,
    SHUTDOWN,
    CANCEL,
    OPEN_DIRECTORY,
    OPEN_FILE,
    READ_WRITE,
    FSYNC,
    POLL_ADD,
    POLL_UPDATE,
    MKDIR,
    RENAME,
    LINK,
    SYMLINK,
    UNLINK,
    STATX,
    CONNECT,
    ACCEPT,

    ;
}
