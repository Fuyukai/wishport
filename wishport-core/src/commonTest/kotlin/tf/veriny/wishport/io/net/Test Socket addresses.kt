package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.io.fs.runWithClosingScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class `Test Socket addresses` {
    @Test
    fun `Test getting bound socket address`() = runWithClosingScope {
        Imperatavize.cancellable<Unit, Fail> {
            val address = Inet4SocketAddress.loopback(SocketType.STREAM, SocketProtocol.TCP, 43512U)
            val sock = Socket(it, address).q()
            sock.setSocketOption(SO_REUSEADDR, true)
            sock.bind(address)

            val addr = sock.getLocalAddress().q()
            assertEquals(address, addr, "expected socket addresses to be equal")
            Cancellable.empty()
        }
    }

    @Test
    fun `Test getting an address on an unconnected socket fails`() = runWithClosingScope {
        assertFailureWith(TransportEndpointIsNotConnected) {
            Socket(it, SocketFamily.IPV4, SocketType.STREAM, SocketProtocol.TCP)
                .andThen { it.getRemoteAddress() }
        }
    }

    @Test
    fun `Test remote socket address`() = runWithClosingScope { assertSuccess {
        Imperatavize.cancellable<Unit, Fail> {
            val address = Inet4SocketAddress.loopback(SocketType.STREAM, SocketProtocol.TCP, 3512U)
            val server = Socket(it, address).q()
            server.setSocketOption(SO_REUSEADDR, true)
            val client = Socket(it, address).q()

            server.bind(address).q()
            server.listen(1).q()

            val incomingRes = Nursery { n ->
                n.startSoon { client.connect(address) }
                server.acceptInto(it)
            }

            assertTrue(incomingRes.isSuccess, "accept failed??? ${incomingRes.getFailures()}")
            val incoming = incomingRes.get()!!
            val remote = incoming.getRemoteAddress().q() as Inet4SocketAddress
            assertEquals(address.address, remote.address, "expected incoming address to be the same")

            Cancellable.empty()
        }
    } }
}
