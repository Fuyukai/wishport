/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(LowLevelApi::class)
@file:Suppress("ClassName")

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(LowLevelApi::class)
class `Test Nurseries` {
    @Test
    fun `Test opening an empty nursery`() = runUntilCompleteNoResult {
        Nursery.open {
            Cancellable.empty()
        }
    }

    @Test
    fun `Test spawning multiple child tasks`() = runUntilCompleteNoResult {
        var result = 0

        Nursery.open {
            for (i in 0 until 3) {
                it.startSoonNoResult {
                    result += 1
                }
            }
        }

        assertEquals(3, result)
    }

    @Test
    fun `Test that cancelling a nursery cancels child tasks`() = runUntilCompleteNoResult {
        var result = 0

        Nursery.open {
            for (i in 0 until 3) {
                it.startSoonNoResult {
                    // tasks without a cancel point can't be interrupted so...
                    checkIfCancelled().andThen {
                        result += 1
                        Cancellable.empty()
                    }
                }
            }

            it.cancelScope.cancel()
        }

        assertEquals(0, result)
    }

    @Test
    fun `Test that trying to spawn in a closed nursery fails`() = runUntilCompleteNoResult {
        val n = Nursery { Either.ok(it).notCancelled() }
        assertTrue(n.isSuccess)
        val nursery = n.get()!!

        val res = nursery.startSoonNoResult { }
        assertTrue(res.isFailure)

        Nursery.open {
            it.cancelScope.cancel()
            assertTrue(it.startSoonNoResult {}.isFailure)
        }
    }

    @Test
    fun `Test Nursery start`() = runUntilCompleteNoResult {
        Nursery.open {
            val result = it.start { ts: TaskStatus<Int> ->
                ts.started(1)
                sleepForever()
            }

            assertTrue(result.isSuccess)
            assertEquals(1, result.get())
            // make sure the internal task is still running
            assertEquals(1, it.openTasks)
            // now kill it
            it.cancelScope.cancel()
        }
    }

    @Test
    fun `Test Nursery start with cancellation`() = runUntilCompleteNoResult {
        Nursery.open { n ->
            // open shielded child scope to prevent the cancellation propagating out to us
            CancelScope.open(shield = true) { _ ->
                val result = n.start { ts: TaskStatus<Unit> ->
                    n.cancelScope.cancel()
                    sleepForever()
                }
                assertTrue(result.isFailure)
                assertEquals(StartTaskWasCancelled, result.getFailure())
            }
        }
    }

}