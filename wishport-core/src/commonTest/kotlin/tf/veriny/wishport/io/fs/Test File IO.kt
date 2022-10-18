/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
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
                .andAlso { println("write").run { it.writeFrom("one".encodeToByteArray()) } }
                .andAlso { println("read").run { it.readInto(buf) } }
                .andThen { println("close").run { it.close() } }
        }

        assertContentEquals("one".encodeToByteArray(), buf)
    }

    // checks for metadata relative
    @OptIn(Unsafe::class)
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
                        .andThen {
                            handle.getMetadata(path)
                        }
                }

                assertEquals(4UL, result.fileSize)

                Cancellable.empty()
            }
        }
    }
}
