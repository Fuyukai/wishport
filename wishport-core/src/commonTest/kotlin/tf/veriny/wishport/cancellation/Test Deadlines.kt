/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.cancellation

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.NS_PER_SEC
import tf.veriny.wishport.getCurrentTime
import tf.veriny.wishport.isCancelled
import tf.veriny.wishport.runUntilCompleteNoResult
import tf.veriny.wishport.waitUntilRescheduled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests cancellation scope deadlines.
 */
@OptIn(LowLevelApi::class)
class `Test Deadlines` {
    @Test
    public fun `Test two second deadline with realtime clock`() = runUntilCompleteNoResult {
        CancelScope.open {
            val previous = getCurrentTime()
            it.localDeadline = previous + (2L * NS_PER_SEC)

            assertTrue(waitUntilRescheduled().isCancelled)
            assertTrue(it.permanentlyCancelled)
            assertEquals(CancelScope.ALWAYS_CANCELLED, it.effectiveDeadline)

            assertTrue(getCurrentTime() >= (previous + 2L * NS_PER_SEC))
        }
    }

    @Test
    public fun `Test effective deadline of child scope`() = runUntilCompleteNoResult {
        CancelScope.open {
            val dl = getCurrentTime() + (30L * NS_PER_SEC)
            it.localDeadline = dl

            CancelScope.open { inner ->
                // impl note: never cancelled is always greater than every deadline, so this
                // also functions as a check for if the DL is gt than the parent one (no).
                assertEquals(CancelScope.NEVER_CANCELLED, inner.localDeadline)
                assertEquals(dl, inner.effectiveDeadline)

                inner.localDeadline = getCurrentTime() + (5L * NS_PER_SEC)
                assertTrue("inner dl should be lt outer deadline") {
                    inner.effectiveDeadline < dl
                }
            }
        }
    }

    @Test
    public fun `Test effective deadline of child scope when cancelling parent`() =
        runUntilCompleteNoResult {
            CancelScope.open {
                it.cancel()

                CancelScope.open { inner ->
                    assertEquals(CancelScope.ALWAYS_CANCELLED, inner.effectiveDeadline)
                }
            }
        }

    @Test
    public fun `Test inner scope is cancelled when parent scope expires`() =
        runUntilCompleteNoResult {
            CancelScope.open {
                it.localDeadline = getCurrentTime() + (2L * NS_PER_SEC)

                CancelScope.open { inner ->
                    assertTrue(waitUntilRescheduled().isCancelled)
                    assertFalse(inner.cancelCalled)
                    assertTrue(inner.permanentlyCancelled)
                }
            }
        }

    @Test
    fun `Test deadlines when it comes to shielding`() = runUntilCompleteNoResult {
        CancelScope.open {
            val dl = (getCurrentTime() + (30L * NS_PER_SEC))
            it.localDeadline = dl

            CancelScope.open { inner ->
                assertEquals(dl, inner.effectiveDeadline)
                inner.shield = true
                assertEquals(
                    CancelScope.NEVER_CANCELLED, inner.effectiveDeadline,
                    "inner scope's DL should now be never cancelled"
                )
            }
        }
    }
}
