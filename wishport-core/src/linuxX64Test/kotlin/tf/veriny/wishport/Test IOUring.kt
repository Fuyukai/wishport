/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.io.FileOpenMode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class `Test IOUring` {
    @OptIn(Unsafe::class, LowLevelApi::class)
    @Test
    public fun `Test reading a file with io_uring`() = runUntilCompleteNoResult {
        val io = EventLoop.get().ioManager

        val file = io.openFilesystemFile(
            null, b("/dev/zero"),
            FileOpenMode.READ_ONLY, setOf()
        )

        assertTrue(file.isSuccess, "file open failed")
        val fd = file.get()!!

        // fill with 1s so we know reading was successful
        val buf = ByteArray(8) { 1 }
        val result = io.read(
            fd, buf, 8U, 0UL, 0
        )
        assertTrue(result.isSuccess, "file read failed with ${result.getFailure()}")
        assertEquals(8, result.get()?.count)
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0), buf)
    }
}
