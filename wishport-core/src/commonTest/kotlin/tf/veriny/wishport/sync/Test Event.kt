/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("ClassName")
@OptIn(LowLevelApi::class)
class `Test Event` {
    // uses a cancel scope + auto jump clock to prevent deadlocking all the tests

    @Test
    fun `Test waiting on event`() = runUntilCompleteNoResult {
        val ev = Event()
        var success = false

        Nursery.open {
            it.startSoon { ev.wait().also { success = true } }

            waitUntilAllTasksAreBlocked()
            assertFalse(success, "child task didn't get blocked on event")

            ev.set()
            waitUntilAllTasksAreBlocked()
            assertTrue(success, "child task didn't wake up for event")
        }
    }

    @Test
    fun `Test waiting on already set event`() = runAutojump {
        val ev = Event()
        ev.set()

        val res = CancelScope {
            it.localDeadline = getCurrentTime() + 1 * NS_PER_SEC
            ev.wait()
        }

        assertTrue(res.isSuccess, "event wasn't waited on properly")
    }
}
