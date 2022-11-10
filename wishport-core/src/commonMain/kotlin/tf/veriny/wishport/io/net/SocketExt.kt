package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe

/**
 * Accepts an incoming [Socket] connection, and adds it to the specified [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun Socket.acceptInto(scope: AsyncClosingScope): CancellableResourceResult<Socket> {
    return accept().andAddTo(scope)
}