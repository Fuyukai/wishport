/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.FasterCloseable
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SeekPosition
import tf.veriny.wishport.io.streams.*

/**
 * A [PartialStream] that wraps a [FilesystemHandle].
 */
@ProvisionalApi
public class UnbufferedFile(
    public val handle: FilesystemHandle<*, *>,
) : PartialStream, FasterCloseable {
    override val closed: Boolean by handle::closed
    override val closing: Boolean by handle::closing
    override var damaged: Boolean = false
        private set

    override fun provideHandleForClosing(): IOHandle? {
        return (handle as? FasterCloseable)?.provideHandleForClosing()
    }

    override fun notifyClosed() {
        (handle as? FasterCloseable)?.notifyClosed()
    }

    public suspend fun seek(
        position: Long,
        whence: SeekWhence
    ): CancellableResult<SeekPosition, Fail> {
        return handle.seek(position, whence)
    }

    override suspend fun writeMost(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return handle.writeMost(buffer, byteCount, bufferOffset)
    }

    override suspend fun writeAll(buffer: ByteArray, byteCount: UInt, bufferOffset: Int): CancellableResult<Unit, Fail> {
        if (damaged) return Cancellable.failed(StreamDamaged)
        return handle.writeAll(buffer, byteCount, bufferOffset) { damaged = true }
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
}
