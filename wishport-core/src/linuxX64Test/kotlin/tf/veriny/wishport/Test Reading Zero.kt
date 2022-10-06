/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.io.fs.FileHandle
import tf.veriny.wishport.io.fs.PosixPurePath
import tf.veriny.wishport.io.fs.openFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests reading /dev/zero on Linux. This is a basic test for io_uring, rather than higher-level
 * filesystem functionality, but it uses the higher-level APIs.
 */
class `Test Reading Zero` {
    @Test
    fun `Test reading dev zero`() = runUntilCompleteNoResult {
        ClosingScope {
            val path = PosixPurePath.from("/dev/zero").get()!!
            val res = FileHandle.openFile(it, path)
            assertTrue(res.isSuccess)
            val handle = res.get()!!
            val buf = ByteArray(8) { 1 }

            val readResult = handle.readInto(
                buf, 4U, 4, 0U
            )
            assertTrue(readResult.isSuccess)
            assertEquals(4, readResult.get()!!.count)
            assertContentEquals(byteArrayOf(1, 1, 1, 1, 0, 0, 0, 0), buf)
        }
    }
}
