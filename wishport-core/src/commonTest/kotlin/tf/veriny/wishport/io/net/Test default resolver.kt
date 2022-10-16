/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.assertFailureWith
import tf.veriny.wishport.assertSuccess
import tf.veriny.wishport.getAddressFromName
import tf.veriny.wishport.runUntilCompleteNoResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class `Test default resolver` {
    @Test
    fun `Test resolving addresses`() = runUntilCompleteNoResult {
        val result = assertSuccess {
            getAddressFromName<StreamEndpointInfo>(
                "one.one.one.one",
                443,
                SocketType.STREAM,
                SocketFamily.IPV4,
                SocketProtocol.TCP
            )
        }

        assertEquals(2, result.size, "expected two IPs")
        val addr = result.first()
        assertTrue(addr is Inet4SocketAddress)
        val ip = addr.address
        assertTrue(ip.toString() == "1.1.1.1" || ip.toString() == "1.0.0.1")
    }

    @Test
    fun `Test resolving invalid addresses`() = runUntilCompleteNoResult {
        assertFailureWith(NameOrServiceNotKnown) {
            getAddressFromName<Nothing>("abcdef.notatld", 0, SocketType.STREAM)
        }
    }
}
