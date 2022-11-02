package tf.veriny.wishport.io.net

import platform.posix.socket
import tf.veriny.wishport.io.Fd
import tf.veriny.wishport.io.IOHandle

public actual fun makeSocket(
    family: SocketFamily,
    type: SocketType,
    protocol: SocketProtocol
): IOHandle {
    return Fd(socket(family.number, type.number, protocol.number))
}