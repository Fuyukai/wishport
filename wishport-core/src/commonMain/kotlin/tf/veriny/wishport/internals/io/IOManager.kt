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
     * Closes an [IOHandle] asynchronously. This method is uncancellable; you must wait for it to
     * return.
     */
    public suspend fun closeHandle(handle: IOHandle): CancellableResourceResult<Empty>

    // CreateFileEx isn't async so we have to punt it off to a worker on windows.

    /**
     * Opens a directory on the real filesystem, returning a directory handle. If [dirHandle] is
     * provided, the directory will be opened relative to the other directory (using openat()
     * semantics).
     */
    @Unsafe
    public suspend fun openFilesystemDirectory(
        dirHandle: DirectoryHandle?,
        path: ByteString
    ): CancellableResourceResult<DirectoryHandle>

    /**
     * Opens a file on the real filesystem, returning a file handle. If [dirHandle] is
     * provided, the file will be opened relative to the provided directory (using openat()
     * semantics).
     */
    @Unsafe
    public suspend fun openFilesystemFile(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResourceResult<RawFileHandle>

    /**
     * Reads [size] bytes from a [IOHandle] into [out], starting at [fileOffset] from the
     * file's current position, and at [bufferOffset] into the provided buffer.
     */
    public suspend fun read(
        handle: IOHandle,
        out: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int,
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Writes [size] bytes from [buf] into an [IOHandle], starting from [bufferOffset] in the
     * provided buffer, and into [fileOffset] from the file's current position.
     */
    public suspend fun write(
        handle: IOHandle,
        buf: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail>

    // FlushFileEx is not async
    /**
     * Forces a file flush for the specified [handle]. If [withMetadata] is specified, then all
     * metadata will be flushed too. Otherwise, only metadata required for read consistency is
     * flushed. (This can be thought of as like fsync() vs fdatasync()).
     */
    public suspend fun fsync(
        handle: IOHandle,
        withMetadata: Boolean
    ): CancellableResourceResult<Empty>

    // io_uring has io_uring_prep_poll_add/poll_remove
    // but windows has these absolute bastard methods in winsock that suck fuck to use
    // WSAPoll 1) only supports 512 sockets (not true) 2) is O(n) (lol)
    // WaitForEvent shit is just terrible overall
    // IOCP doesn't work properly by default!
    // so instead we just do what all the cool kids (read: trio) does and talk directly to AFD, the
    // kernel socket driver. if M$ doesn't want us doing this stuff, they should stop encouraging
    // usage of the Nt functions in their docs instead.

    /**
     * Polls a handle for readiness. Returns the set of events the handle is ready for.
     */
    public suspend fun pollHandle(
        handle: IOHandle,
        what: Set<Poll>
    ): CancellableResourceResult<PollResult>
}
