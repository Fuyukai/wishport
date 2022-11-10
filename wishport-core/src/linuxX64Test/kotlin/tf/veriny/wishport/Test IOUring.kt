/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.core.*
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.io.Poll
import tf.veriny.wishport.io.PollResult
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.io.net.*
import tf.veriny.wishport.sync.Event
import tf.veriny.wishport.sync.Promise
import kotlin.test.*

/**
 * Tests reading /dev/zero on Linux. This is a basic test for io_uring, rather than higher-level
 * filesystem functionality, but it uses the higher-level APIs.
 */
class `Test IOUring` {
    @Test
    fun `Test reading dev zero`() = runUntilCompleteNoResult {
        AsyncClosingScope {
            val path = PosixPurePath.from("/dev/zero").get()!!
            val res = FilesystemHandle.openFile(it, path)
            assertTrue(res.isSuccess)
            val handle = res.get()!!
            val buf = ByteArray(8) { 1 }

            val readResult = handle.readInto(
                buf, 4U, 4, 0U
            )
            assertTrue(readResult.isSuccess)
            assertEquals(4U, readResult.get()!!.count)
            assertContentEquals(byteArrayOf(1, 1, 1, 1, 0, 0, 0, 0), buf)
        }
    }

    /**
     * Tests cancelling an io_uring result.
     */
    @OptIn(LowLevelApi::class, Unsafe::class)
    @Test
    fun `Test io_uring cancellation`() = runUntilCompleteNoResult {
        val addr = Inet4SocketAddress(
            SocketType.STREAM, SocketProtocol.TCP,
            IPv4Address.of("127.0.0.1").expect(), 7777U
        )

        val sock = Socket(addr).expect()
        sock.bind(addr)
        sock.listen()

        val p = Promise<CancellableResourceResult<PollResult>>()

        Nursery.open { n ->
            val io = getIOManager()
            // POLL_READ on a server socket works for waiting until accept()
            n.startSoonNoResult {
                val res = io.pollHandle(sock.raw, setOf(Poll.POLL_READ))
                p.set(res)
            }

            waitUntilAllTasksAreBlocked()
            CancelScope.open(shield = true) {
                n.cancelScope.cancel()
                waitUntilAllTasksAreBlocked()

                val i = p.wait()
                assertTrue(i.isSuccess)
                assertTrue(i.get()!!.isCancelled)
                assertEquals(0, io.pendingItems)
            }
        }
    }

    // something something test api not impl
    // kys though
    @OptIn(LowLevelApi::class, Unsafe::class)
    @Test
    fun `Test submitting when the queue is really small`() {
        val loop = EventLoop.new(ioManagerSize = 4)
        // the actual manager size is *2 fyi so we have to spawn a lot
        val task = suspend {
            val manager = getIOManager()
            manager.openFilesystemFile(
                null, b("/dev/zero"),
                FileOpenType.READ_ONLY, setOf(), setOf()
            ).andThen { manager.closeHandle(it) }
        }

        loop.runUntilComplete {
            Nursery.open { n ->
                repeat(512) { n.startSoon(task) }
            }

            Cancellable.empty()
        }
    }

    @OptIn(LowLevelApi::class)
    @Test
    fun `Test submitting a linked request`() = runWithClosingScope { scope ->
        val addr = Inet4SocketAddress(
            SocketType.STREAM, SocketProtocol.TCP,
            IPv4Address.of("127.0.0.1").expect(), 7777U
        )

        Nursery.open { n ->
            // synchronise point
            val startup = Event()
            val shutdown = Event()

            // server code
            n.startSoonNoResult { assertFailureWith(BrokenPipe) {
                Socket(scope, addr)
                    .andAlso { it.setSocketOption(SO_REUSEADDR, true) }
                    .andAlso { it.bind(addr) }
                    .andAlso { it.listen(1).notCancelled() }
                    .also { startup.set() }
                    .andThen {
                        it.acceptInto(scope)
                    }
                    .andAlso { shutdown.wait() }
                    .andThen {
                        repeatedly {
                            it.writeFrom(b("should fail eventually!").toByteArray())
                        }
                    }
            } }

            // client code
            n.startSoonNoResult {
                Socket(scope, addr)
                    .andAlso { startup.wait() }
                    .andAlso {
                        it.connect(addr)
                    }
                    .andAlso {
                        it.close().also { shutdown.set() }
                    }
            }
        }
    }
}
