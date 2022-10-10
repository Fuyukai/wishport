/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(LowLevelApi::class)

package tf.veriny.wishport.sync

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.core.open
import tf.veriny.wishport.core.startSoonNoResult
import tf.veriny.wishport.getFailure
import tf.veriny.wishport.runUntilCompleteNoResult
import tf.veriny.wishport.waitUntilAllTasksAreBlocked
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [Lock] class.
 */
class `Test Lock` {
    @Test
    fun `Test that locking works`() = runUntilCompleteNoResult {
        val lock = Lock(Unit)
        var ran = false

        // acquire the lock from the outer task, causing the inner to block
        Nursery.open {
            lock.runWhilstLockedNoResult { _ ->
                it.startSoonNoResult {
                    lock.runWhilstLockedNoResult { ran = true }
                }

                waitUntilAllTasksAreBlocked()
                assertFalse(ran, "child task somehow bypassed lock")
            }

            waitUntilAllTasksAreBlocked()
            assertTrue(ran, "child task is still blocked?")
        }
    }

    @Test
    fun `Test that fifo locking is fair`() = runUntilCompleteNoResult {
        val lock = FIFOLock(Unit)
        val items = mutableListOf<Int>()

        // spawn inside lock, wait until they're all blocked, then release and watch them
        // run in fifo
        Nursery.open { n ->
            lock.runWhilstLockedNoResult { _ ->
                repeat(3) {
                    n.startSoonNoResult { lock.runWhilstLockedNoResult { _ -> items.add(it) } }
                }

                waitUntilAllTasksAreBlocked()
                assertTrue(items.isEmpty(), "items should be empty but isn't")
            }
        }

        assertEquals(listOf(0, 1, 2), items)
    }

    @Test
    fun `Test that lock is not reeentrant`() = runUntilCompleteNoResult {
        val lock = Lock(Unit)
        lock.runWhilstLockedNoResult {
            val res = lock.runWhilstLocked { Cancellable.empty() }
            assertEquals(LockAlreadyOwned, res.getFailure())
        }
    }
}
