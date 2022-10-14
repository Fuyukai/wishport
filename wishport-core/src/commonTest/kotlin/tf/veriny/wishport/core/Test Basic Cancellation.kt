/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("ClassName")

package tf.veriny.wishport.core

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.checkIfCancelled
import tf.veriny.wishport.checkpoint
import tf.veriny.wishport.isCancelled
import tf.veriny.wishport.runUntilCompleteNoResult
import kotlin.test.Test

/**
 * Tests that cancellation works.
 */
@OptIn(LowLevelApi::class)
class `Test Basic Cancellation` {
    @Test
    fun `Test cancelling ourselves`() = runUntilCompleteNoResult {
        val x = CancelScope.open {
            assert(!checkIfCancelled().isCancelled)
            it.cancel()
            assert(checkIfCancelled().isCancelled)
        }
    }

    @Test
    fun `Test cancelling after a checkpoint`() = runUntilCompleteNoResult {
        val x = CancelScope.open {
            checkpoint()
            it.cancel()

            assert(checkpoint().isCancelled)
        }
    }

    @Test
    fun `Test nested scopes`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            assert(!checkIfCancelled().isCancelled)

            val innerRes = CancelScope { inner ->
                inner.cancel()
                checkIfCancelled()
            }

            assert(innerRes.isCancelled)

            // inner cancellation should have no effect on us
            assert(!checkIfCancelled().isCancelled)
        }
    }

    @Test
    fun `Test cancelling outer scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open { inner ->
                assert(!inner.cancelCalled)
                assert(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    fun `Test cancelling outer scope from inner scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            CancelScope.open { inner ->
                outer.cancel()
                assert(!inner.cancelCalled)
                assert(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    fun `Test shielding`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open(shield = true) {
                assert(it.shield)
                assert(!checkIfCancelled().isCancelled) { "task is cancelled, but it should not be" }
            }
        }
    }

    // ensures that a shielded cancel scope that becomes unshielded is then cancelled.
    @Test
    fun `Test unshielding`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open(shield = true) {
                assert(!checkIfCancelled().isCancelled) { "task is cancelled, but it should not be" }
                it.shield = false
                assert(checkIfCancelled().isCancelled) { "task is not cancelled, but it should be" }
            }
        }
    }

    @Test
    fun `Ensure unshielding is permanent`() = runUntilCompleteNoResult {
        CancelScope.open { first ->
            first.cancel()

            CancelScope.open(shield = true) {
                assert(!checkIfCancelled().isCancelled)
                it.shield = false
                assert(checkIfCancelled().isCancelled)
                it.shield = true
                assert(checkIfCancelled().isCancelled)
            }
        }
    }
}
