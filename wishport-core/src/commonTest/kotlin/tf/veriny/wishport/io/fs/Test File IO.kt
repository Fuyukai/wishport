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
import kotlin.test.assertTrue

internal inline fun runWithClosingScope(crossinline block: suspend (AsyncClosingScope) -> Unit) {
    runUntilCompleteNoResult {
        AsyncClosingScope {
            block(it)
        }
    }
}

@OptIn(ProvisionalApi::class)
class `Test File IO` {
    @Test
    fun `Test writing to and reading from a file`() = runWithClosingScope { scope ->
        val buf = ByteArray(3)
        val result = openTemporaryFile(scope)
            .andAlso { println("write").run { it.writeFrom("one".encodeToByteArray()) } }
            .andAlso { println("read").run { it.readInto(buf) } }
            .andThen { println("close").run { it.close() } }

        assertTrue(result.isSuccess, "file read/write failed: ${result.getFailure()}")
        assertContentEquals("one".encodeToByteArray(), buf)
    }
}