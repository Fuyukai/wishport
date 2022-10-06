/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.IndexOutOfRange
import tf.veriny.wishport.TooSmall
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.io.ByteCountResult
import tf.veriny.wishport.internals.io.IOHandle

/**
 * Defines any object that is file-like, i.e. allows reading and writing as if it was a file.
 * This includes filesystem files, sockets, pipes, and certain Linux APIs.
 */
public interface FileLikeHandle {
    /** The raw system file handle for this FileHandle, for usage in backend I/O. */
    @LowLevelApi
    public val raw: IOHandle

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
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail>
}
