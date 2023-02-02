/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("ClassName", "ObjectPropertyName", "ConstPropertyName")

package tf.veriny.wishport.io.net

private const val _SO_DEBUG = 1
private const val _SO_REUSEADDR = 2
private const val _SO_KEEPALIVE = 9
private const val _SO_BROADCAST = 6
private const val _SO_OOBINLINE = 10
private const val _SOL_SOCKET = 1
private const val _SO_TYPE = 3
private const val _SO_PROTOCOL = 38

/**
 * Sealed interface over the recognised socket option types.
 */
public sealed class SocketOption<T>(
    /** The linux-level option name for this option. */
    public val optname: Int,

    /** The level param of setsockopt(2). */
    public val level: Int,
)

public class BooleanSocketOption(optname: Int, level: Int) : SocketOption<Boolean>(optname, level)
public class UIntSocketOption(optname: Int, level: Int) : SocketOption<UInt>(optname, level)

/**
 * This option toggles recording of debugging information in the underlying protocol modules.
 */
public val SO_DEBUG: SocketOption<Boolean> = BooleanSocketOption(_SO_DEBUG, _SOL_SOCKET)

/**
 * This option allows a second application to re-bind to this port before the TIME_WAIT
 * period is up if this socket is ungracefully closed.
 */
public val SO_REUSEADDR: SocketOption<Boolean> = BooleanSocketOption(_SO_REUSEADDR, _SOL_SOCKET)

/**
 * This option controls whether the underlying protocol should periodically transmit messages
 * on a connected socket. If the peer fails to respond to these messages, the connection is
 * considered broken.
 */
public val SO_KEEPALIVE: SocketOption<Boolean> = BooleanSocketOption(_SO_KEEPALIVE, _SOL_SOCKET)

/**
 * This option controls if broadcast packets can be sent over this socket. This has no effect
 * on IPv6 sockets.
 */
public val SO_BROADCAST: SocketOption<Boolean> = BooleanSocketOption(_SO_BROADCAST, _SOL_SOCKET)

/**
 * If this option is set, out-of-band data received on the socket is placed in the normal input
 * queue.
 */
public val SO_OOBINLINE: SocketOption<Boolean> = BooleanSocketOption(_SO_OOBINLINE, _SOL_SOCKET)

/**
 * This can be used in getsockopt() to get the type that the socket was created as.
 */
public val SO_TYPE: SocketOption<UInt> = UIntSocketOption(_SO_TYPE, _SOL_SOCKET)

/**
 * This can be used in getsockopt() to get the protocol that the socket was created as.
 */
public val SO_PROTOCOL: SocketOption<UInt> = UIntSocketOption(_SO_PROTOCOL, _SOL_SOCKET)
