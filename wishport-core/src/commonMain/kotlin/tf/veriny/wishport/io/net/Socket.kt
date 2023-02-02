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
public class Socket
private constructor(
    public val family: SocketFamily,
    public val type: SocketType,
    public val protocol: SocketProtocol,
    override val raw: SocketHandle,
) : FileLikeHandle {
    public companion object {
        /**
         * Creates a new [Socket] with the specified socket address, and adds it to [scope].
         */
        @OptIn(Unsafe::class)
        public operator fun invoke(
            scope: AsyncClosingScope,
            address: SocketAddress
        ): ResourceResult<Socket> {
            return Socket(address).andAddTo(scope)
        }

        /**
         * Creates a new [Socket] with the specified socket parameters, and adds it to [scope].
         */
        @OptIn(Unsafe::class)
        public operator fun invoke(
            scope: AsyncClosingScope,
            family: SocketFamily,
            type: SocketType,
            protocol: SocketProtocol,
        ): ResourceResult<Socket> {
            return Socket(family, type, protocol).andAddTo(scope)
        }

        /**
         * Creates a new [Socket] with the specified socket parameters, and adds it to [scope].
         */
        @Unsafe
        public operator fun invoke(
            family: SocketFamily,
            type: SocketType,
            protocol: SocketProtocol,
        ): ResourceResult<Socket> {
            return makeSocket(family, type, protocol).andThen {
                Either.ok(Socket(family, type, protocol, it))
            }
        }

        /**
         * Creates a new [Socket], using the parameters specified by the [SocketAddress].
         */
        @Unsafe
        public operator fun invoke(address: SocketAddress): ResourceResult<Socket> {
            return makeSocket(address.family, address.type, address.protocol)
                .andThen {
                    Either.ok(Socket(address.family, address.type, address.protocol, it))
                }
        }
    }

    override var closed: Boolean = false
        private set

    override var closing: Boolean = false
        private set

    /**
     * Gets the remote address for this socket.
     */
    public fun getRemoteAddress(): ResourceResult<SocketAddress> {
        return getRemoteAddress(raw, type, protocol)
    }

    /**
     * Gets the local address this socket is bound to.
     */
    public fun getLocalAddress(): ResourceResult<SocketAddress> {
        return getLocalAddress(raw, type, protocol)
    }

    /**
     * Binds this socket to the specified address.
     */
    public suspend fun bind(address: SocketAddress): CancellableResourceResult<Empty> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.bind(raw, address)
    }

    /**
     * Connects this socket to the specified address.
     */
    public suspend fun connect(address: SocketAddress): CancellableResourceResult<Empty> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.connect(raw, address)
    }

    /**
     * Accepts a new incoming connection, producing a new [Socket].
     */
    @Unsafe
    public suspend fun accept(): CancellableResourceResult<Socket> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.accept(raw).andThen {
            Cancellable.ok(Socket(family, type, protocol, it))
        }
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
        if (closed || closing) return Cancellable.empty()

        closing = true

        return CancelScope(shield = true) {
            val io = getIOManager()
            // ignore return value, suppress error
            io.closeSocket(raw).also { closed = true }
        }.andThen { Cancellable.empty() }
    }
}
