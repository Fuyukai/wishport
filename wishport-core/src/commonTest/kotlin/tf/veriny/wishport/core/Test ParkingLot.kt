/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(LowLevelApi::class)
@file:Suppress("ClassName")

package tf.veriny.wishport.core

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.isCancelled
import tf.veriny.wishport.moveOnAfter
import tf.veriny.wishport.runUntilCompleteNoResult
import tf.veriny.wishport.waitUntilAllTasksAreBlocked
import kotlin.test.*

class `Test ParkingLot` {
    @Test
    fun `Test parking and unparking single task`() = runUntilCompleteNoResult {
        val lot = ParkingLot()
        var variable = false

        Nursery.open {
            it.startSoonNoResult {
                lot.park()
                variable = true
            }

            waitUntilAllTasksAreBlocked()
            assertFalse(variable)
            lot.unpark()
        }

        assertTrue(variable)
    }

    @Test
    fun `Test unparking one by one`() = runUntilCompleteNoResult {
        val lot = ParkingLot()
        // le box has arrived
        val items = mutableListOf<Int>()

        Nursery.open {
            for (i in 0 until 5) {
                it.startSoonNoResult {
                    lot.park()
                    items.add(i)
                }
            }

            waitUntilAllTasksAreBlocked()
            assertEquals(5, it.openTasks, "there should be 5 tasks running")
            assertEquals(0, items.size, "items should be empty!")

            while (it.openTasks > 0) {
                lot.unpark()
                waitUntilAllTasksAreBlocked()

                assertEquals(
                    4 - it.openTasks, items.last(),
                    "tasks weren't unparked in the right order"
                )
            }
        }
    }

    @Test
    fun `Test unparking all at once`() = runUntilCompleteNoResult {
        val lot = ParkingLot()
        val items = mutableListOf<Int>()

        Nursery.open {
            for (i in 0 until 5) {
                it.startSoonNoResult {
                    lot.park()
                    items.add(i)
                }
            }

            waitUntilAllTasksAreBlocked()
            lot.unparkAll()
        }

        assertContentEquals(mutableListOf(0, 1, 2, 3, 4), items)
    }

    @Test
    fun `Test cancellation removes parked task`() = runAutojump {
        val lot = ParkingLot()

        val res = moveOnAfter(1_000_000_000) {
            lot.park()
        }

        assertTrue(res.isCancelled)
        assertEquals(0, lot.parkedCount)
    }
}
