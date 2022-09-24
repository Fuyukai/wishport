/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("ClassName")

package tf.veriny.wishport.cancellation

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.checkIfCancelled
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.isCancelled
import tf.veriny.wishport.runUntilCompleteNoResult
import kotlin.test.Test

/**
 * Tests that cancellation works.
 */
@OptIn(LowLevelApi::class)
public class `Test Basic Cancellation` {
    @Test
    public fun `Test cancelling ourselves`() = runUntilCompleteNoResult {
        val x = CancelScope.open {
            assert(!checkIfCancelled().isCancelled)
            it.cancel()
            assert(checkIfCancelled().isCancelled)
        }
    }

    @Test
    public fun `Test nested scopes`() = runUntilCompleteNoResult {
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
    public fun `Test cancelling outer scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            outer.cancel()

            CancelScope.open { inner ->
                assert(!inner.cancelCalled)
                assert(checkIfCancelled().isCancelled)
            }
        }
    }

    @Test
    public fun `Test cancelling outer scope from inner scope`() = runUntilCompleteNoResult {
        CancelScope.open { outer ->
            CancelScope.open { inner ->
                outer.cancel()
                assert(!inner.cancelCalled)
                assert(checkIfCancelled().isCancelled)
            }
        }
    }
}
