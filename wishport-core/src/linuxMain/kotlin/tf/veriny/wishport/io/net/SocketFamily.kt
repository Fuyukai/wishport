/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import platform.posix.*

// not exhaustive; if anyone actually needs any of the more esoteric AF_ families let me know.
public actual enum class SocketFamily(public actual val number: Int) {
    UNIX_DOMAIN_SOCKET(AF_UNIX),
    IPV4(AF_INET),
    IPV6(AF_INET6),

    /**
     * A Netlink socket. Used for communicating with the Linux kernel for configuring Netlink
     * facilities. See ``netlink(7)`` for details.
     */
    NETLINK(PF_NETLINK),

    /**
     * A raw packet socket. Used for sending and receiving raw data at the OSI Layer 2 level.
     * See ``packet(7)`` for details.
     */
    PACKET(PF_PACKET),

    /**
     * A kernel cryptography socket. Used for talking to the Linux kernel cryptography APIs.
     */
    ALG(PF_ALG)
    ;
}
