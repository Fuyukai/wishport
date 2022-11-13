/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.andThen
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.ShutdownHow
import tf.veriny.wishport.io.streams.PartialStream
import tf.veriny.wishport.io.streams.StreamDamaged
import tf.veriny.wishport.io.streams.writeAll
import tf.veriny.wishport.io.streams.writeMost

/**
 * A [PartialStream] that wraps a [Socket].
 */
@ProvisionalApi
public class TcpSocketStream
public constructor(public val sock: Socket) : PartialStream {
    override val closed: Boolean by sock::closed
    override val closing: Boolean by sock::closing

    override var damaged: Boolean = false
        private set

    override suspend fun readIntoUpto(
        buf: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return sock.readInto(buf, byteCount, bufferOffset)
    }

    override suspend fun writeMost(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> {
        return sock.writeMost(buffer, byteCount, bufferOffset)
    }

    override suspend fun writeAll(
        buffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): CancellableResult<Unit, Fail> {
        if (damaged) return Cancellable.failed(StreamDamaged)

        return sock.writeAll(buffer, byteCount, bufferOffset) { damaged = true }
    }

    override suspend fun sendEof(): CancellableResult<Unit, Fail> {
        return sock.shutdown(ShutdownHow.WRITE).andThen { Cancellable.empty() }
    }

    override suspend fun close(): CancellableResult<Unit, Fail> {
        return sock.close()
    }
}
