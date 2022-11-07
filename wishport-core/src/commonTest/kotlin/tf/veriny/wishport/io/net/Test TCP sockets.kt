/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.collections.decodeToString
import tf.veriny.wishport.io.fs.runWithClosingScopeThis
import tf.veriny.wishport.io.readUpto
import kotlin.test.Test
import kotlin.test.assertTrue

private val DAYTIME_REGEX = "\\d{5} \\d{2}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\d{2} \\d \\d \\d{3}\\.\\d UTC\\(NIST\\) \\*".toRegex()

@OptIn(ProvisionalApi::class)
class `Test TCP sockets` {
    @Test
    fun `Test basic TCP connection`() = runWithClosingScopeThis {
        val ip = IPv6Address.of("2610:20:6f15:15::27").expect("this is a valid address")
        val address = Inet6SocketAddress(SocketProtocol.TCP, SocketType.STREAM, ip, 13U)

        val data = assertSuccess {
            openTcpStream(address)
                .andThen {
                    it.readUpto(2048U)
                }
        }.decodeToString()

        assertTrue(data.contains("UTC(NIST)"), "wtf: $data")
    }
}
