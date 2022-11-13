/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.core.getCurrentTask
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.internals.getIdealBlockSize
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.FasterCloseable
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SeekPosition
import tf.veriny.wishport.io.streams.BufferedPartialStream
import tf.veriny.wishport.io.streams.EofNotSupported

// TODO: Make this more generic. A "BufferedX" impl would be extremely useful.

/**
 * A wrapper around a system file that automatically buffers reads.
 */
@OptIn(LowLevelApi::class)
@ProvisionalApi
public class BufferedFile
private constructor(
    public val handle: SystemFilesystemHandle,
    public val bufferSize: UInt,
) : BufferedPartialStream, FasterCloseable {
    public companion object {
        @OptIn(LowLevelApi::class)
        public suspend operator fun invoke(
            handle: SystemFilesystemHandle
        ): CancellableResult<BufferedFile, Fail> {
            return handle.getIdealBlockSize()
                .andThen { Cancellable.ok(BufferedFile(handle, it)) }
        }
    }

    private val buffer = ByteArray(bufferSize.toInt())
    private var front = 0U
    private var end = 0U

    private val backing = UnbufferedFile(handle)
    override val closed: Boolean by backing::closed
    override val closing: Boolean by backing::closing
    override val damaged: Boolean by backing::damaged

    override fun provideHandleForClosing(): IOHandle {
        return handle.provideHandleForClosing()
    }

    override fun notifyClosed() {
        handle.notifyClosed()
    }

    /**
     * Seeks this file to the specified [position], using the behaviour specified by [whence].
     */
    public suspend fun seek(
        position: Long,
        whence: SeekWhence
    ): CancellableResult<SeekPosition, Fail> {
        return backing.seek(position, whence)
    }

    private fun readFromBuffer0(into: ByteArray, byteCount: UInt, bufferOffset: Int): UInt {
        return if (end > front) {
            val amount = minOf(end - front, byteCount)
            buffer.copyInto(into, bufferOffset, front.toInt(), (front + amount).toInt())
            front += amount
            amount
        } else {
            0U
        }
    }

    @OptIn(LowLevelApi::class)
    override fun readFromBuffer(into: ByteArray, byteCount: UInt, bufferOffset: Int): UInt {
        if (into.checkBuffers(byteCount, bufferOffset).isFailure) {
            return 0U
        }

        return readFromBuffer0(into, byteCount, bufferOffset)
    }

    override suspend fun close(): CancellableResult<Unit, Fail> {
        return backing.close()
    }

    override suspend fun sendEof(): CancellableResult<Unit, Fail> {
        return Cancellable.failed(EofNotSupported)
    }

    override suspend fun writeMost(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return backing.writeMost(buffer, byteCount, bufferOffset)
    }

    override suspend fun writeAll(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<Unit, Fail> {
        return backing.writeAll(buffer, byteCount, bufferOffset)
    }

    override suspend fun readIntoUpto(
        buf: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        val task = getCurrentTask()

        // this is absolutely fuck ugly
        return task.checkIfCancelled()
            .andThen { buf.checkBuffers(byteCount, bufferOffset).notCancelled() }
            .andThen {
                // eagerly copy from the buffer first
                var needed = byteCount
                var total = 0U
                total += readFromBuffer0(buf, needed, bufferOffset)
                needed -= total

                // buffer emptied, refill it
                if (needed > 0U) {
                    front = 0U
                    end = 0U
                    // more than the buffer size, just pass through the read to the I/O directly.
                    val res = if (needed > bufferSize) {
                        backing.readIntoUpto(
                            buf,
                            needed,
                            bufferOffset + total.toInt()
                        )
                    } else {
                        // less than the buffer size, fill in the buffer
                        backing.readIntoUpto(buffer, bufferSize, 0)
                            .andThen {
                                end = it.count
                                Cancellable.ok(
                                    ByteCountResult(
                                        readFromBuffer0(
                                            buf,
                                            needed,
                                            bufferOffset + total.toInt()
                                        )
                                    )
                                )
                            }
                    }

                    // suppress any errors if we read any data from the buffer.
                    if (res.isSuccess) {
                        Cancellable.ok(ByteCountResult(total + res.get()!!.count))
                    } else if (total > 0U) {
                        Cancellable.ok(ByteCountResult(total))
                    } else res
                } else {
                    uncancellableCheckpoint(ByteCountResult(total))
                }
            }
    }
}
