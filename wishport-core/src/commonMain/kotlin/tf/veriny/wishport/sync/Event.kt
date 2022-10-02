/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled

/**
 * An object that can be waited upon by a task until it is set by another task.
 */
@OptIn(LowLevelApi::class)
public class Event {
    private val tasks = mutableSetOf<Task>()
    public var flag: Boolean = false
        private set

    /** The number of tasks currently waiting on this event. */
    public val waiters: Int get() = tasks.size

    /**
     * Sets the flag for this event, and wakes up any tasks that are waiting for it.
     */
    public fun set() {
        if (flag) return
        flag = true

        for (task in tasks) {
            task.reschedule()
        }

        // empty strong refs
        tasks.clear()
    }

    /**
     * Waits for this event to be set by another task. If the flag is already set, this will issue
     * a checkpoint and immediately return.
     */
    public suspend fun wait(): CancellableEmpty {
        val task = getCurrentTask()
        return task.checkIfCancelled()
            .andThen {
                if (flag) uncancellableCheckpoint()
                else {
                    tasks.add(task)
                    task.suspendTask()
                }
            }.also { tasks.remove(task) } // only relevant if waitUntilRescheduled gets cancelled
    }
}
