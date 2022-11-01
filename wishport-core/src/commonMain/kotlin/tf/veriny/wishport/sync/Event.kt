/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.ParkingLot
import tf.veriny.wishport.core.getCurrentTask
import tf.veriny.wishport.internals.checkpoint

/**
 * An object that can be waited upon by a task until it is set by another task.
 */
@OptIn(LowLevelApi::class)
public class Event {
    private val lot = ParkingLot()

    public var flag: Boolean = false
        private set

    /** The number of tasks currently waiting on this event. */
    public val waiters: Int get() = lot.parkedCount

    /**
     * Sets the flag for this event, and wakes up any tasks that are waiting for it.
     */
    public fun set() {
        if (flag) return
        flag = true

        lot.unparkAll()
    }

    /**
     * Waits for this event to be set by another task. If the flag is already set, this will issue
     * a checkpoint and immediately return.
     */
    public suspend fun wait(): CancellableEmpty {
        val task = getCurrentTask()

        return if (flag) task.checkpoint(Unit)
        else lot.park(task)
    }
}
