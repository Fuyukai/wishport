/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.channel

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(LowLevelApi::class)
class `Test Memory Channels` {
    @Test
    fun `Test sending data between two channels`() = runUntilCompleteNoResult {
        val (read, write) = openMemoryChannelPair<Int>()
        Nursery.open {
            it.startSoon { write.send(1) }
            val data = read.receive().expect("shoulda gotten some data")
            assertEquals(1, data)
        }
    }

    @Test
    fun `Test sending data blocks`() = runAutojump {
        val (read, write) = openMemoryChannelPair<Unit>()

        assertCancelled {
            moveOnAfter(1L * NS_PER_SEC) { write.send(Unit) }
        }
    }

    @Test
    fun `Test channel buffered sends`() = runAutojump {
        val (read, write) = openMemoryChannelPair<Int>(1)
        assertSuccess {
            moveOnAfter(1L * NS_PER_SEC) {
                write.send(1)
            }
        }

        assertEquals(1, read.statistics().bufferedItems)

        val res = moveOnAfter(1L * NS_PER_SEC) { write.send(1) }
        assertTrue(res.isCancelled, "send should have blocked forever")
        assertEquals(1, read.statistics().bufferedItems)

        val item = read.receive().expect()
        assertEquals(0, read.statistics().bufferedItems)
    }

    @Test
    fun `Test closing this end of the channel`() = runUntilCompleteNoResult {
        val (_, write) = openMemoryChannelPair<Unit>()
        write.close()

        assertFailureWith(AlreadyClosedError) { write.send(Unit) }
    }

    @Test
    fun `Test closing the other end of the channel`() = runUntilCompleteNoResult {
        val (read, write) = openMemoryChannelPair<Unit>()
        read.close()

        assertFailureWith(AlreadyClosedError) { write.send(Unit) }
    }

    @Test
    fun `Test closing whilst receiving`() = runUntilCompleteNoResult {
        val (read, write) = openMemoryChannelPair<Unit>()

        Nursery.open {
            it.startSoonNoResult {
                assertFailureWith(AlreadyClosedError) { read.receive() }
            }

            waitUntilAllTasksAreBlocked()
            write.close()
        }
    }

    @Test
    fun `Test cloning the receive channel`() = runUntilCompleteNoResult {
        val (read, write) = openMemoryChannelPair<Int>(2)
        val clone = read.clone().expect("clone should succeed")

        Imperatavize.cancellable<Unit, Nothing> {
            write.send(1).q()
            write.send(2).q()
        }

        assertEquals(1, assertSuccess { read.receive() })
        assertEquals(2, assertSuccess { clone.receive() })
    }

    @Test
    fun `Test cloning then closing`() = runUntilCompleteNoResult {
        val (read, write) = openMemoryChannelPair<Int>(1)
        val newWrite = write.clone().expect("clone should succeed")
        write.close()

        assertSuccess { newWrite.send(1) }
        assertEquals(1, assertSuccess { read.receive() })
    }

    @Test
    fun `Test round-robin sending`() = runUntilCompleteNoResult {
        val items = mutableListOf<Int>()

        val (read, write) = openMemoryChannelPair<Int>()
        Nursery.open {
            repeat(5) { c ->
                val chan = write.clone().expect()
                it.startSoonNoResult {
                    chan.send(c)
                    chan.close()
                }

                // wait for the chan.send call to block
                // this means we avoid task step order, and the send wake up is down to the channel
                // rescheduling.
                waitUntilAllTasksAreBlocked()
            }

            repeat(5) {
                items.add(read.receive().expect("expected the channel to keep reading"))
            }
        }

        write.close()
        assertEquals((0 until 5).toList(), items)
        // make sure all the cloned channels were closed
        assertFailureWith(AlreadyClosedError) { read.receive() }
    }
}
