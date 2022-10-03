/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.*
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.core.open
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class `Test CapacityLimiter` {
    @Test
    fun `Test that capacity limiter actually limits`() = runUntilCompleteNoResult {
        val limiter = CapacityLimiter(3, Unit)
        val waiter = Event()
        var done = false

        // gross synchronisation stuff
        // starts 3 tasks that hold the capacity limiter open, then starts a 4th task that
        // will set done.
        Nursery.open {
            repeat(3) { _ ->
                it.startSoon {
                    limiter.unwrapAndRun {
                        waiter.wait()
                        Cancellable.empty()
                    }
                }
            }

            it.startSoon {
                limiter.unwrapAndRun {
                    done = true; Cancellable.empty()
                }
            }

            waitUntilAllTasksAreBlocked()
            assertFalse(done, "second task woke up when it shouldn't've")
            assertEquals(0, limiter.currentTokens)

            waiter.set()
            waitUntilAllTasksAreBlocked()
            assertTrue(done, "second task didn't wake up when it should've")
        }
    }

    @Test
    fun `Test capacity limiter is not reentrant`() = runUntilCompleteNoResult {
        val limiter = CapacityLimiter(1, Unit)

        val res = limiter.unwrapAndRun {
            limiter.unwrapAndRun { Cancellable.empty() }
        }

        assertEquals(AlreadyAcquired, res.getFailure())

        val res2 = limiter.unwrapAndRun { Cancellable.empty() }
        assertTrue(res2.isSuccess)
    }
}
