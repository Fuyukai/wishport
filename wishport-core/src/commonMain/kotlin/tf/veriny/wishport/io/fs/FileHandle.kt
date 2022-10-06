/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.io.ByteCountResult
import tf.veriny.wishport.internals.io.Empty
import tf.veriny.wishport.internals.io.IOHandle
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

/**
 * A handle to an opened file on a filesystem.
 */
public interface FileHandle<F : PurePath<F>> : Closeable {
    public companion object;

    /** The filesystem this handle is open on. */
    public val filesystem: Filesystem<F>

    /** The raw system file handle for this FileHandle, for usage in backend I/O. */
    @LowLevelApi
    public val raw: IOHandle

    /** The path to this file. */
    public val path: F

    /**
     * Opens a file relative to this file if (and only if) this file is a directory. This will
     * fail with ENOTDIR otherwise.
     */
    @Unsafe
    public suspend fun openRelative(
        path: F,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResult<FileHandle<F>, Fail>

    /**
     * Reads data from the file handle into the specified [buf]. The data will be read from the file
     * at offset [fileOffset], for [size] bytes, and copied into the buffer at [bufferOffset].
     *
     * If any of these are out of bounds, then this will return [IndexOutOfRange] or [TooSmall].
     */
    public suspend fun readInto(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Writes data from [buf] into the specified file handle. The data will be read from the buffer
     * at offset [bufferOffset], into the file at [fileOffset], for [size] bytes.
     *
     * If any of these are out of bounds, then this will return [IndexOutOfRange] or [TooSmall].
     */
    public suspend fun writeFrom(
        buf: ByteArray, size: UInt, bufferOffset: Int, fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Flushes the data written into this file to disk. If [withMetadata] is true, then all file
     * metadata will be flushed; otherwise, only essential metadata relating to write consistency
     * will be flushed.
     */
    public suspend fun flush(withMetadata: Boolean = true): CancellableResourceResult<Empty>
}
