/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.helpers

import tf.veriny.wishport.SecureRandom
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.AutojumpClock
import tf.veriny.wishport.core.NS_PER_SEC

/**
 * A manually controllable clock, useful for testing. This clock has two behaviours:
 *
 * 1. Completely manual control, where the clock's time is constant and is only controlled by
 *    [advance].
 * 2. Autojump mode, where the clock's time is automatically set to the next deadline when the
 *    event loop goes to sleep.
 */
@LowLevelApi
public class ManualClock(public var autojump: Boolean = false) : AutojumpClock {
    private var time: Long = SecureRandom.nextLong(
        0x0000_0000_FFFF_FFFF, 0x0000_00FF_FFFF_FFFF
    )

    public fun advance(seconds: Int = 0, nanoseconds: Int = 0) {
        time += (seconds * NS_PER_SEC) + nanoseconds
    }

    override fun autojump(nextDeadline: Long) {
        if (autojump) time = nextDeadline
    }

    override fun getCurrentTime(): Long {
        return time
    }

    // never sleep
    override fun getSleepTime(nextDeadline: Long): Long {
        return 0
    }
}
