/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Clock

@LowLevelApi
public actual object PlatformClock : Clock {
    override fun getCurrentTime(): Long {
        return getMonotonicTime()
    }

    override fun getSleepTime(nextDeadline: Long): Long {
        val x = nextDeadline - getMonotonicTime()
        return if (x < 0) 0L
        else x
    }
}
