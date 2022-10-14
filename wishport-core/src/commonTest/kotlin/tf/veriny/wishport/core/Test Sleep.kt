/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.sleep
import tf.veriny.wishport.sleepUntil
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("ClassName")
@OptIn(LowLevelApi::class)
class `Test Sleep` {
    @Test
    fun `Test sleeping for N seconds`() = runAutojump {
        val before = it.getCurrentTime()
        sleep(10L * NS_PER_SEC)
        val after = it.getCurrentTime()
        assertEquals(10L * NS_PER_SEC, after - before)
    }

    @Test
    fun `Test sleeping until absolute time`() = runAutojump {
        val before = it.getCurrentTime()
        sleepUntil(before + 5L * NS_PER_SEC)
        val after = it.getCurrentTime()

        assertEquals(5L * NS_PER_SEC, after - before)
    }

    @Test
    fun `Test sleeping with incorrect times`() = runAutojump {
        val before = it.getCurrentTime()
        sleepUntil(5L * NS_PER_SEC)
        val after = it.getCurrentTime()

        assertEquals(before, after)
    }
}
