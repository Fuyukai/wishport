package tf.veriny.wishport.io.net

import platform.posix.SOCK_DGRAM
import platform.posix.SOCK_RAW
import platform.posix.SOCK_STREAM

public actual enum class SocketType(public actual val number: Int) {
    STREAM(SOCK_STREAM),
    DGRAM(SOCK_DGRAM),
    RAW(SOCK_RAW),
    ;
}