/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.getIOManager
import tf.veriny.wishport.io.*

/**
 * A BSD socket that can either listen to new incoming connections or start outgoing connections.
 */
@OptIn(LowLevelApi::class)
public class Socket<T : SocketAddress>
private constructor(override val raw: IOHandle) : FileLikeHandle {
    public companion object {
        /**
         * Creates a new [Socket], using the parameters specified by the [SocketAddress].
         */
        @Unsafe
        public operator fun <T : SocketAddress> invoke(address: T): ResourceResult<Socket<T>> {
            return makeSocket(address.family, address.type, address.protocol)
                .andThen { Either.ok(Socket(it)) }
        }
    }

    override var closed: Boolean = false
        private set

    /**
     * Binds this socket to the specified address.
     */
    public suspend fun bind(address: T): CancellableResourceResult<Empty> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.bind(raw, address)
    }

    /**
     * Connects this socket to the specified address.
     */
    public suspend fun connect(address: T): CancellableResourceResult<Empty> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.connect(raw, address)
    }

    /**
     * Starts this socket listening with the specified [backlog] of possible open connections.
     */
    public fun listen(backlog: Int = 128): ResourceResult<Unit> {
        if (closed) return Either.err(AlreadyClosedError)

        return listen(raw, backlog)
    }

    @OptIn(Unsafe::class)
    public fun <Opt : Any> getSocketOption(
        option: SocketOption<Opt>
    ): ResourceResult<Opt> {
        if (closed) return Either.err(AlreadyClosedError)

        return getSocketOption(raw, option)
    }

    /**
     * Sets a socket option on this socket.
     */
    @OptIn(Unsafe::class)
    public fun <Opt : Any> setSocketOption(
        option: SocketOption<Opt>,
        value: Opt
    ): ResourceResult<Unit> {
        if (closed) return Either.err(AlreadyClosedError)

        return setSocketOption(raw, option, value)
    }

    override suspend fun readInto(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.recv(raw, buf, size, bufferOffset, 0)
    }

    override suspend fun writeFrom(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.send(raw, buf, size, bufferOffset, 0)
    }

    public suspend fun shutdown(how: ShutdownHow): CancellableResourceResult<Empty> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.shutdown(raw, how)
    }

    @OptIn(LowLevelApi::class)
    override suspend fun close(): CancellableResult<Unit, Fail> {
        return if (!closed) {
            CancelScope(shield = true) {
                val io = getIOManager()
                // ignore return value, suppress error
                io.shutdown(raw, ShutdownHow.BOTH)
                io.closeHandle(raw).also { closed = true }
            }.andThen { Cancellable.empty() }
        } else {
            Cancellable.empty()
        }
    }
}
