package tf.veriny.wishport.io.net

/**
 * Enumeration of the possible socket family types.
 */
public expect enum class SocketFamily {
    /**
     * A unix domain socket. This can be used for local communication with other processes on the
     * system.
     */
    UNIX_DOMAIN_SOCKET,

    /**
     * An IP version 4 socket, for communication over local or remote networks.
     */
    IPV4,

    /**
     * An IP version 6 socket, for communication over local or remote networks. Note that server
     * sockets of this type can also listen for IPv4 connections.
     */
    IPV6,
    ;

    public val number: Int
}