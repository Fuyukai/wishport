package tf.veriny.wishport.io.net

import tf.veriny.wishport.assertSuccess
import tf.veriny.wishport.expect
import tf.veriny.wishport.io.fs.runWithClosingScope
import kotlin.test.Test
import kotlin.test.assertTrue

class `Test Socket Options` {
    @Test
    fun `Test boolean socket option`() = runWithClosingScope { scope ->
        val sock = Socket(scope, SocketFamily.IPV4, SocketType.STREAM, SocketProtocol.TCP)
            .expect("socket creation shouldn't fail")

        assertSuccess { sock.setSocketOption(SO_REUSEADDR, true) }
        assertTrue(assertSuccess { sock.getSocketOption(SO_REUSEADDR) })
    }

}