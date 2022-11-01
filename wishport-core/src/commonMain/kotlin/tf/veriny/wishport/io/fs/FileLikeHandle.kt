/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SeekPosition

/**
 * Defines any object that is file-like, i.e. allows reading and writing as if it was a file.
 * This includes filesystem files, sockets, pipes, and certain Linux APIs.
 */
public interface FileLikeHandle : AsyncCloseable {
    /** The raw system file handle for this FileHandle, for usage in backend I/O. */
    @LowLevelApi
    public val raw: IOHandle

    /**
     * Reads data from the file handle into the specified [buf]. The data will be read from the file
     * at offset [fileOffset], for [size] bytes, and copied into the buffer at [bufferOffset].
     *
     * If [fileOffset] is not specified, then it will use the current file position.
     *
     * If any of these are out of bounds, then this will return [IndexOutOfRange] or [TooSmall].
     */
    public suspend fun readInto(
        buf: ByteArray,
        size: UInt = buf.size.toUInt(),
        bufferOffset: Int = 0,
        fileOffset: ULong = ULong.MAX_VALUE
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Writes data from [buf] into the specified file handle. The data will be read from the buffer
     * at offset [bufferOffset], into the file at [fileOffset], for [size] bytes.
     *
     * If [fileOffset] is not specified, then it will use the current file position.
     *
     * If any of these are out of bounds, then this will return [IndexOutOfRange] or [TooSmall].
     */
    public suspend fun writeFrom(
        buf: ByteArray,
        size: UInt = buf.size.toUInt(),
        bufferOffset: Int = 0,
        fileOffset: ULong = ULong.MAX_VALUE
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Seeks this file to the specified [position], using the behaviour specified by [whence].
     */
    public suspend fun seek(
        position: Long,
        whence: SeekWhence
    ): CancellableResult<SeekPosition, Fail>
}
