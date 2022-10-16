/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

// TODO: even more type annotations or some shit

/**
 * Defines a way for a socket to connect to an endpoint. This wraps a set of [SocketAddress]
 * instances that can be used for a happy-eyeballs connect.
 *
 * All the [SocketAddress]es wrapped inside this object will be of the same [SocketType].
 */
public abstract class EndpointInfo(
    addresses: Set<SocketAddress>,
) : Set<SocketAddress> by addresses {
    /** The type of the socket created for this endpoint. */
    public abstract val type: SocketType
}

/**
 * The endpoint info for stream (TCP) sockets.
 */
public class StreamEndpointInfo(addresses: Set<SocketAddress>) : EndpointInfo(addresses) {
    override val type: SocketType
        get() = SocketType.STREAM
}

/**
 * The endpoint info for datagram (UDP) sockets.
 */
public class DatagramEndpointInfo(addresses: Set<SocketAddress>) : EndpointInfo(addresses) {
    override val type: SocketType
        get() = SocketType.DGRAM
}

/**
 * The endpoint info for raw sockets.
 */
public class RawEndpointInfo(addresses: Set<SocketAddress>) : EndpointInfo(addresses) {
    override val type: SocketType
        get() = SocketType.RAW
}
