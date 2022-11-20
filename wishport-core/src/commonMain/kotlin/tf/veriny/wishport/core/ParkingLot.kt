/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.helpers.TaskList
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled

// https://github.com/python-trio/trio/blob/master/trio/_core/_parking_lot.py

/**
 * A fair waiting queue for building concurrency primitives on top of.
 */
@OptIn(LowLevelApi::class)
public class ParkingLot {
    private val tasks = TaskList<Task>()

    /** The number of tasks currently parked here. */
    public val parkedCount: Int get() = tasks.size

    private inline fun popMany(count: Int, cb: (Task) -> Unit) {
        var cursor = 0
        while (cursor < count) {
            val item = tasks.removeFirst() ?: break
            cb(item)
            cursor++
        }
    }

    /**
     * Parks the current task, with the task passed in. Not meant for general usage!
     */
    @LowLevelApi
    public suspend fun park(task: Task): CancellableEmpty {
        return task.checkIfCancelled()
            .andThen {
                tasks.append(task)
                waitUntilRescheduled() as CancellableEmpty
            }
            .also { tasks.removeTask(task) }
    }

    /**
     * Parks the current task until a call to [unpark] is made.
     */
    public suspend fun park(): CancellableEmpty {
        val task = getCurrentTask()
        return park(task)
    }

    /**
     * Unparks one or more tasks that are currently waiting.
     */
    public fun unpark(count: Int = 1) {
        if (count <= 0) return
        popMany(count) { it.reschedule(Cancellable.empty()) }
    }

    /**
     * Unparks all the tasks in this depot.
     */
    public fun unparkAll() {
        popMany(tasks.size) { it.reschedule(Cancellable.empty()) }
    }
}
