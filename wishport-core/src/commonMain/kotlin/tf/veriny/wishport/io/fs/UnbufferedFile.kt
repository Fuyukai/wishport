/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.*
import tf.veriny.wishport.io.streams.*
import tf.veriny.wishport.sync.ConflictDetector

/**
 * A [PartialStream] that wraps a [FilesystemHandle].
 */
@ProvisionalApi
public class UnbufferedFile<F : PP<F>, M : FM>(
    public val handle: FilesystemHandle<F, M>,
) : PartialStream, FasterCloseable, Flushable by handle, Seekable by handle {
    /** The path to this file. */
    public val path: F by handle::path

    override val closed: Boolean by handle::closed
    override val closing: Boolean by handle::closing
    override var damaged: Boolean = false
        private set

    private val conflict = ConflictDetector(Unit)

    override fun provideHandleForClosing(): IOHandle? {
        return (handle as? FasterCloseable)?.provideHandleForClosing()
    }

    override fun notifyClosed() {
        (handle as? FasterCloseable)?.notifyClosed()
    }

    override suspend fun writeMost(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return conflict.use {
            handle.writeMost(buffer, byteCount, bufferOffset)
        }
    }

    override suspend fun writeAll(buffer: ByteArray, byteCount: UInt, bufferOffset: Int): CancellableResult<Unit, Fail> {
        if (damaged) return Cancellable.failed(StreamDamaged)

        return conflict.use {
            handle.writeAll(buffer, byteCount, bufferOffset) { damaged = true }
        }
    }

    override suspend fun close(): CancellableResult<Unit, Fail> {
        return handle.close()
    }

    override suspend fun sendEof(): CancellableResult<Unit, Fail> {
        return Cancellable.failed(EofNotSupported)
    }

    override suspend fun readIntoUpto(
        buf: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return handle.readInto(buf, byteCount, bufferOffset, ULong.MAX_VALUE)
    }

    /**
     * Reads the entirety of this file, returning a [ByteString].
     */
    @OptIn(Unsafe::class)
    public suspend fun readUntilEof(): CancellableResult<ByteString, Fail> {
        return handle.filesystem.getFileMetadata(handle)
            .andThen {
                handle.readUntilEof(it.size.toUInt())
            }
            .andThen { Cancellable.ok(ByteString.uncopied(it)) }
    }
}
