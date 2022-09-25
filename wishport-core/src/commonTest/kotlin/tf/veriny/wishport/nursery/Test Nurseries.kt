/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(LowLevelApi::class)

package tf.veriny.wishport.nursery

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.core.open
import tf.veriny.wishport.core.startSoonNoResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class `Test Nurseries` {
    @Test
    public fun `Test opening an empty nursery`() = runUntilCompleteNoResult {
        Nursery.open {
            Cancellable.empty()
        }
    }

    @Test
    public fun `Test spawning multiple child tasks`() = runUntilCompleteNoResult {
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
    public fun `Test that cancelling a nursery cancels child tasks`() = runUntilCompleteNoResult {
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
    public fun `Test that trying to spawn in a closed nursery fails`() = runUntilCompleteNoResult {
        val n = Nursery { Either.ok(it).notCancelled() }
        assertTrue(n.isSuccess)
        val nursery = n.get()!!

        val res = nursery.startSoonNoResult { }
        assertTrue(res.isFailure)
    }
}
