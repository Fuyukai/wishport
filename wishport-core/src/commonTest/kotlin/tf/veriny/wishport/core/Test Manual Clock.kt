/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.getCurrentTime
import tf.veriny.wishport.helpers.ManualClock
import tf.veriny.wishport.internals.getMonotonicTime
import tf.veriny.wishport.runUntilCompleteNoResult
import tf.veriny.wishport.sleep
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(LowLevelApi::class)
internal fun runAutojump(fn: suspend (ManualClock) -> Unit) {
    val clock = ManualClock(autojump = true)
    runUntilCompleteNoResult(clock) { fn(clock) }
}

/**
 * Tests that the manual clock works.
 */
public class `Test Manual Clock` {
    @OptIn(LowLevelApi::class)
    @Test
    public fun `Test sleeping with the auto jump clock`() =
        runAutojump {
            // TODO: wait for me to write my own kotlinx.datetime that doesnt suck major balls
            //       and then use it
            val realWallTime = getMonotonicTime()
            val prevTime = getCurrentTime()
            sleep(30L * NS_PER_SEC)

            // make sure we didn't really sleep for 30s
            assertTrue((getMonotonicTime() - realWallTime) / NS_PER_SEC < 30)
            assertTrue((getCurrentTime() - prevTime) / NS_PER_SEC >= 30)
        }
}
