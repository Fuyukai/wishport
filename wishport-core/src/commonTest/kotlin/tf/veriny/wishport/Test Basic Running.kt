/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.getCurrentTask
import tf.veriny.wishport.core.reschedule
import tf.veriny.wishport.core.waitUntilRescheduled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@Suppress("ClassName")
@OptIn(LowLevelApi::class)
class `Test Basic Running` {
    @Test
    fun `Test that a simple non-suspending function works`() {
        val result = assertSuccess { runUntilComplete { Either.ok(1).notCancelled() } }
        assertEquals(1, result)
    }

    @Test
    fun `Test suspending and rescheduling a function`() {
        runUntilComplete {
            val task = getCurrentTask()
            reschedule(task)
            waitUntilRescheduled()

            Either.ok(Unit).notCancelled()
        }

        // no need for asserts as this automatically works
    }

    @Test
    fun `Test that an exception destroys the event loop`() {
        assertFails {
            runUntilComplete<Nothing, Nothing> { throw Throwable("abcdef") }
        }
    }
}
