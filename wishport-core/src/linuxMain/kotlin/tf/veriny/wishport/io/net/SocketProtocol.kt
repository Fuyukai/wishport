package tf.veriny.wishport.io.net

import platform.posix.IPPROTO_IP
import platform.posix.IPPROTO_RAW
import platform.posix.IPPROTO_TCP
import platform.posix.IPPROTO_UDP

public actual enum class SocketProtocol(public actual val number: Int) {
    UNSPECIFIED(IPPROTO_IP),
    TCP(IPPROTO_TCP),
    UDP(IPPROTO_UDP),
    RAW(IPPROTO_RAW),
    ;
}