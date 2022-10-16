package tf.veriny.wishport.io.net

/**
 * A single address for creating and connecting a socket.
 */
public interface SocketAddress {
    /** The family used to create the socket. */
    public val family: SocketFamily

    /** The type used to create the socket. */
    public val type: SocketType

    /** The protocol for the socket. */
    public val protocol: SocketProtocol
}

public abstract class BaseSocketAddress(
    override val family: SocketFamily,
    override val protocol: SocketProtocol,
    override val type: SocketType,
) : SocketAddress

/**
 * A socket address that uses IPv4.
 */
public class Inet4SocketAddress(
    protocol: SocketProtocol, type: SocketType,
    public val address: IPv4Address, public val port: Int
) : BaseSocketAddress(SocketFamily.IPV4, protocol, type)

public class Inet6SocketAddress(
    protocol: SocketProtocol, type: SocketType,
    public val address: IPv6Address, public val port: Int
) : BaseSocketAddress(SocketFamily.IPV6, protocol, type)