/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.EofNotSupported
import tf.veriny.wishport.io.PartialStream
import tf.veriny.wishport.io.StreamDamaged

/**
 * A [PartialStream] that wraps a [FilesystemHandle].
 */
@ProvisionalApi
public class UnbufferedFileStream<F : PurePath<F>>(
    public val handle: FilesystemHandle<F>,
) : PartialStream {
    override val closed: Boolean by handle::closed
    override var damaged: Boolean = false
        private set

    override suspend fun sendMost(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        var runningTotal = 0U

        while (runningTotal < byteCount) {
            val result = handle.writeFrom(
                buffer, byteCount - runningTotal, bufferOffset, 0UL
            )
            if (result.isCancelled) {
                return if (runningTotal > 0U) Cancellable.ok(ByteCountResult(runningTotal.toInt()))
                else result
            } else if (result.isFailure) return result
            else {
                runningTotal += result.get()!!.count.toUInt()
            }
        }

        return Cancellable.ok(ByteCountResult(runningTotal.toInt()))
    }

    override suspend fun sendAll(buffer: ByteArray, byteCount: UInt, bufferOffset: Int): CancellableResult<Unit, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)
        if (damaged) return Cancellable.failed(StreamDamaged)

        var runningTotal = 0U

        while (runningTotal < byteCount) {
            val result = handle.writeFrom(
                buffer, byteCount - runningTotal, bufferOffset, 0UL
            )

            if (result.isCancelled) {
                runningTotal += result.get()!!.count.toUInt()
            } else {
                damaged = true
                // safe cast, <Cancellable> isn't part of us
                @Suppress("UNCHECKED_CAST")
                return result as CancellableResult<Unit, Fail>
            }
        }

        return Cancellable.empty()
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
        return handle.readInto(buf, byteCount, bufferOffset, 0U)
    }
}
