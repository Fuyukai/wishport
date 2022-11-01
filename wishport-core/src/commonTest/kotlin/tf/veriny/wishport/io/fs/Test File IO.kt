/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal inline fun runWithClosingScope(crossinline block: suspend (AsyncClosingScope) -> Unit) {
    runUntilCompleteNoResult {
        AsyncClosingScope {
            block(it)
        }
    }
}

@Suppress("ClassName")
@OptIn(ProvisionalApi::class)
class `Test File IO` {
    @Test
    fun `Test writing to and reading from a file`() = runWithClosingScope { scope ->
        val buf = ByteArray(3)
        assertSuccess {
            openTemporaryFile(scope)
                .andAlso { println("write").run { it.writeAll("one".encodeToByteArray()) } }
                .andAlso { println("seek").run { it.seek(0, SeekWhence.SEEK_SET) } }
                .andAlso { println("read").run { it.readIntoUpto(buf) } }
                .andThen { println("close").run { it.close() } }
        }

        assertContentEquals("one".encodeToByteArray(), buf)
    }

    // accidentally had a long-running bug where the file would always be read from offset zero...
    @Test
    fun `Test reading from a file uses the right file offset`() = runWithClosingScope { scope ->
        val first = ByteArray(2)
        val second = ByteArray(2)
        assertSuccess {
            openTemporaryFile(scope)
                .andAlso { it.writeAll("data".encodeToByteArray()) }
                .andAlso { it.seek(0, SeekWhence.SEEK_SET) }
                .andAlso { it.readIntoUpto(first) }
                .andAlso { it.readIntoUpto(second) }
                .andThen { it.close() }
        }

        assertContentEquals("da".encodeToByteArray(), first)
        assertContentEquals("ta".encodeToByteArray(), second)
    }

    // checks for metadata relative
    @Test
    fun `Test getting file metadata`() = runWithClosingScope { scope ->
        val path = systemPathFor("test.txt").get()!!

        assertSuccess {
            createTemporaryDirectory { handle ->
                val result = assertSuccess {
                    handle.openRelative(scope, path, FileOpenType.READ_WRITE, setOf(FileOpenFlags.CREATE_IF_NOT_EXISTS))
                        .andAlso { it.writeFrom("test".encodeToByteArray()) }
                        .andAlso { it.flush() }
                        .andThen { it.close() }
                        .andThen { handle.getMetadata(path) }
                }

                assertEquals(4UL, result.size)

                Cancellable.empty()
            }
        }
    }

    @Test
    fun `Test buffered file IO`() = runWithClosingScope { scope ->
        val path = systemPathFor("test.bin").get()!!

        assertSuccess {
            createTemporaryDirectory { handle ->
                val file = assertSuccess {
                    scope.openBufferedSystemFile(
                        handle, path,
                        FileOpenType.READ_WRITE,
                        flags = setOf(FileOpenFlags.CREATE_IF_NOT_EXISTS)
                    )
                }

                val buffer = ByteArray(file.bufferSize.toInt() * 2)
                SecureRandom.nextBytes(buffer)

                val out = ByteArray(16)

                // this is all wrapped in .andThens to make sure it automatically fails wwith
                // a good message if seek/readintoupto fails
                file.writeAll(buffer)
                    .andThen {
                        println("seek")
                        file.seek(0, SeekWhence.SEEK_SET)
                    }
                    .andThen {
                        println("read")
                        file.readIntoUpto(out)
                    }
                    .andThen {
                        println("equals")
                        assertContentEquals(buffer.sliceArray(0 until 16), out)
                        Cancellable.empty()
                    }
                    .andThen {
                        // make sure that we actually do a buffer read.
                        println("readFromBuffer")
                        val count = file.readFromBuffer(out)
                        assertEquals(16U, count)
                        assertContentEquals(buffer.sliceArray(16 until 32), out)
                        Cancellable.empty()
                    }
            }
        }
    }
}
