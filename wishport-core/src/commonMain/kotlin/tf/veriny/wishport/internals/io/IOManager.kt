/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

// TODO: Rethink if this should be responsible for I/O dispatching itself, or if that functionality
//       should be moved to the event loop, which can then poll this.

/**
 * Responsible for handling I/O within the event loop. This entirely abstracts away how I/O works
 * per-platform, and is responsible internally for its own set of suspended tasks.
 */
@LowLevelApi
@Unsafe
public expect class IOManager : Closeable {
    public companion object {
        public fun default(): IOManager
    }

    /**
     * Peeks off all pending I/O events, and wakes up tasks that would be waiting.
     */
    public fun pollIO()

    /**
     * Waits for I/O forever.
     */
    public fun waitForIO()

    /**
     * Waits for I/O for [timeout] nanoseconds.
     */
    public fun waitForIOUntil(timeout: Long)

    // == actual I/O methods == //
    /**
     * Opens a directory on the real filesystem, returning a directory handle. If [dirHandle] is
     * provided, the directory will be opened relative to the other directory (using openat()
     * semantics).
     */
    public suspend fun openFilesystemDirectory(
        dirHandle: DirectoryHandle?,
        path: ByteString
    ): CancellableResourceResult<DirectoryHandle>

    /**
     * Opens a file on the real filesystem, returning a file handle. If [dirHandle] is
     * provided, the file will be opened relative to the provided directory (using openat()
     * semantics).
     */
    public suspend fun openFilesystemFile(
        dirHandle: DirectoryHandle?, path: ByteString,
        mode: FileOpenMode, flags: Set<FileOpenFlags>
    ): CancellableResourceResult<FileHandle>

    /**
     * Reads [size] bytes from a [ReadableHandle] into [out], starting at [fileOffset] from the
     * file's current position, and at [bufferOffset] into the provided buffer.
     */
    public suspend fun read(
        handle: ReadableHandle,
        out: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int,
    ): CancellableResult<ByteCountResult, Fail>
}
