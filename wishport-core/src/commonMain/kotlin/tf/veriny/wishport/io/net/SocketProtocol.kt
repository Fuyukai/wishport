/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
