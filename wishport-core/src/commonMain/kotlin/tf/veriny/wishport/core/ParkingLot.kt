/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.Task

// https://github.com/python-trio/trio/blob/master/trio/_core/_parking_lot.py

/**
 * A fair waiting queue for building concurrency primitives on top of.
 */
@OptIn(LowLevelApi::class)
public class ParkingLot {
    // xxx: linkedhashmap has no easy way of removing the first element
    //      so we have to reach in with the iterator directly.
    //      the k/n impl seems to be fast enough underneath but i should replace this w/ something
    //      better later
    private val tasks = linkedMapOf<Task, Unit>()

    /** The number of tasks currently parked here. */
    public val parkedCount: Int get() = tasks.size

    private inline fun popMany(count: Int, cb: (Task) -> Unit) {
        val it = tasks.keys.iterator()
        var c = 0
        while (it.hasNext() && c < count) {
            val next = it.next()
            it.remove()
            cb(next)
            c++
        }
    }

    /**
     * Parks the current task until a call to [unpark] is made.
     */
    public suspend fun park(): CancellableEmpty {
        val task = getCurrentTask()

        return checkIfCancelled()
            .andThen {
                tasks[task] = Unit
                waitUntilRescheduled()
            }
            .also { tasks.remove(task) }
    }

    /**
     * Unparks one or more tasks that are currently waiting.
     */
    public fun unpark(count: Int = 1) {
        if (count <= 0) return
        popMany(count) { it.reschedule() }
    }

    /**
     * Unparks all the tasks in this depot.
     */
    public fun unparkAll() {
        popMany(tasks.size) { it.reschedule() }
    }
}
