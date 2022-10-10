package tf.veriny.wishport.io.net

/**
 * An enumeration of supported socket protocols.
 */
public expect enum class SocketProtocol {
    /**
     * The protocol is unspecified; it will be automatically chosen by the socket, if possible.
     */
    UNSPECIFIED,

    /**
     * The protocol for Transmission Control Protocol.
     */
    TCP,

    // hehe
    /**
     * The protocol for Unreliable Datagram Protocol.
     */
    UDP,

    /**
     * The protocol for raw sockets.
     */
    RAW,
    ;

    public val number: Int
}