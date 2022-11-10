/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

/**
 * A single address for creating and connecting a socket.
 */
public sealed interface SocketAddress {
    /** The family used to create the socket. */
    public val family: SocketFamily

    /** The type used to create the socket. */
    public val type: SocketType

    /** The protocol for the socket. */
    public val protocol: SocketProtocol
}

public sealed class BaseSocketAddress(
    override val family: SocketFamily,
    override val type: SocketType,
    override val protocol: SocketProtocol,
) : SocketAddress {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false

        other as BaseSocketAddress
        if (other.family != family) return false
        if (other.type != type) return false
        if (other.protocol != protocol) return false

        return true
    }

    override fun hashCode(): Int {
        var result = family.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

/**
 * A socket address that uses IPv4.
 */
public class Inet4SocketAddress(
    type: SocketType,
    protocol: SocketProtocol,
    public val address: IPv4Address,
    public val port: UShort
) : BaseSocketAddress(SocketFamily.IPV4, type, protocol) {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is Inet4SocketAddress) return false

        return other.protocol == protocol && other.family == family &&
            other.address == address && other.port == port
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + port.hashCode()
        return result
    }
}

public class Inet6SocketAddress(
    type: SocketType,
    protocol: SocketProtocol,
    public val address: IPv6Address,
    public val port: UShort
) : BaseSocketAddress(SocketFamily.IPV6, type, protocol) {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is Inet6SocketAddress) return false

        return other.protocol == protocol && other.family == family &&
            other.address == address && other.port == port
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + port.hashCode()
        return result
    }
}
