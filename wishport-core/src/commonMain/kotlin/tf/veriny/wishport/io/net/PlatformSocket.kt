/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.Either
import tf.veriny.wishport.ResourceError
import tf.veriny.wishport.ResourceResult
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SocketHandle

// These functions are internal and provide no guarantee of safety or stability. They can and will
// be removed at my whim. Any other functionality related to socket handles is available in the
// I/O manager.

// Unsafe: leaks fds
/**
 * Creates a new raw socket handle with the specified socket family, socket type, and socket
 * protocol.
 */
@Unsafe
@LowLevelApi
public expect fun makeSocket(
    family: SocketFamily,
    type: SocketType,
    protocol: SocketProtocol
): ResourceResult<SocketHandle>

// Unsafe: constructing a struct option with the wrong values can cause out of bound reads
/**
 * Sets a socket option on a raw socket handle.
 */
@Unsafe
@LowLevelApi
public expect fun <T : Any> setSocketOption(
    handle: IOHandle,
    option: SocketOption<T>,
    value: T
): Either<Unit, ResourceError>

/**
 * Geta a socket option from a raw socket handle.
 */
@Unsafe
@LowLevelApi
public expect fun <T : Any> getSocketOption(
    handle: IOHandle,
    option: SocketOption<T>
): Either<T, ResourceError>

/**
 * Gets the [SocketAddress] of the peer connected to the specified socket.
 *
 * [type] and [protocol] can be optionally provided to avoid a getsockopt() call, but are
 * only used for filling in the [SocketAddress] parameters for consistency.
 */
@LowLevelApi
public expect fun getRemoteAddress(
    sock: SocketHandle,
    type: SocketType? = null,
    protocol: SocketProtocol? = null,
): ResourceResult<SocketAddress>

/**
 * Gets the [SocketAddress] that this socket is bound to locally.
 *
 * [type] and [protocol] can be optionally provided to avoid a getsockopt() call, but are
 * only used for filling in the [SocketAddress] parameters for consistency.
 */
@LowLevelApi
public expect fun getLocalAddress(
    sock: SocketHandle,
    type: SocketType? = null,
    protocol: SocketProtocol? = null,
): ResourceResult<SocketAddress>


/**
 * Starts listening on a raw socket handle.
 */
@LowLevelApi
public expect fun listen(handle: IOHandle, backlog: Int): ResourceResult<Unit>
