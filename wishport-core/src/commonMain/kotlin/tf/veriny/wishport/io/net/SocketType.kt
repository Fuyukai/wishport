/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

/**
 * Enumeration of supported socket types.
 */
public expect enum class SocketType {
    /**
     * A socket type that provides reliable, in-order, and two-way data streams.
     */
    STREAM,

    /**
     * A socket type that provides fast, unreliable, unordered, and two-way single message
     * transmissions (called datagram).
     */
    DGRAM,

    /**
     * A socket type that provides raw access to writing network packets.
     */
    RAW,
    ;

    public val number: Int
}
