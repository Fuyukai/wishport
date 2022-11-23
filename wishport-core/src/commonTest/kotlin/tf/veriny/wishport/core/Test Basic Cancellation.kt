/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("ClassName")

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that cancellation works.
 */
@OptIn(LowLevelApi::class)
class `Test Basic Cancellation` {
    @Test
    fun `Test cancelling ourselves`() = runUntilCompleteNoResult {
        val x = CancelScope.open {
            assertTrue(!checkIfCancelled().isCancelled)
            it.cancel()
            assertTrue(checkIfCancelled().isCancelled)
        }
    }

    @Test
    fun `Test cancelling after a checkpoint`() = runUntilCompleteNoResult {
        val x = CancelScope.open {
            checkpoint()
            it.cancel()

            assertTrue(checkpoint().isCancelled)
        }
    }

    @Test
    fun `Test nested scopes`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            assertTrue(!checkIfCancelled().isCancelled)

            val innerRes = CancelScope { inner ->
                inner.cancel()
                checkIfCancelled()
            }

            assertTrue(innerRes.isCancelled)

            // inner cancellation should have no effect on us
            assertTrue(!checkIfCancelled().isCancelled)
        }
    }

    @Test
    fun `Test cancelling outer scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open { inner ->
                assertTrue(!inner.cancelCalled)
                assertTrue(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    fun `Test cancelling outer scope from inner scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            CancelScope.open { inner ->
                outer.cancel()
                assertTrue(!inner.cancelCalled)
                assertTrue(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    fun `Test shielding`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open(shield = true) {
                assertTrue(it.shield)
                assertTrue(
                    !checkIfCancelled().isCancelled,
                    "task is cancelled, but it should not be"
                )
            }
        }
    }

    // ensures that a shielded cancel scope that becomes unshielded is then cancelled.
    @Test
    fun `Test unshielding`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open(shield = true) {
                assertTrue(
                    !checkIfCancelled().isCancelled,
                    "task is cancelled, but it should not be"
                )
                it.shield = false
                assertTrue(
                    checkIfCancelled().isCancelled,
                    "task is not cancelled, but it should be"
                )
            }
        }
    }

    @Test
    fun `Ensure unshielding is permanent`() = runUntilCompleteNoResult {
        CancelScope.open { first ->
            first.cancel()

            CancelScope.open(shield = true) {
                assertTrue(!checkIfCancelled().isCancelled)
                it.shield = false
                assertTrue(checkIfCancelled().isCancelled)
                it.shield = true
                assertTrue(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    fun `Test cancellation is suppressed during a reschedule`() = runUntilCompleteNoResult {
        val task = getCurrentTask()
        task.reschedule(Cancellable.ok(123))
        CancelScope.open { scope ->
            scope.cancel()

            val result = waitUntilRescheduled()
            assertFalse(result.isCancelled, "result was cancelled despite reschedule")
            assertEquals(123, result.get()!!)
        }
    }
}
