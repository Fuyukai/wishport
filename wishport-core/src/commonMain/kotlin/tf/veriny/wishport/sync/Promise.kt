/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableSuccess
import tf.veriny.wishport.andThen

/**
 * Like an [Event], but carrying arbitrary data that is returned when awaited on.
 */
public class Promise<T : Any> {
    private val event = Event()
    private lateinit var item: T

    public val flag: Boolean by event::flag

    /** The number of tasks currently waiting on this event. */
    public val waiters: Int by event::waiters

    /**
     * Sets the data for this promise. Like [Event], this can only happen once; subsequent sets are
     * ignored and the data dropped.
     */
    public fun set(data: T) {
        if (event.flag) return

        item = data
        event.set()
    }

    /**
     * Waits for this event to be set by another task. If the flag is already set, this will issue
     * a checkpoint and immediately return.
     */
    public suspend fun wait(): CancellableSuccess<T> {
        return event.wait().andThen { Cancellable.ok(item) }
    }
}
