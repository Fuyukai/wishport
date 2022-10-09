package tf.veriny.wishport.io

import tf.veriny.wishport.AsyncCloseable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi

// TODO: we need preadv() versions, so we likely need to think about making a Buffer class.

/**
 * The base interface for all objects that can send bytes.
 */
@ProvisionalApi
public interface SendStream : AsyncCloseable {
    /**
     * Marks this stream as damaged or not.
     *
     * A damaged stream is one that has been cancelled or otherwise failed during [sendAll].
     * Such streams (for example, TLS streams) become essentially useless after this, as it's not
     * easily possible to know what (if any) data was sent or how to retry the operation.
     *
     * If a stream has been damaged, then all attempts at [sendAll] will return [StreamDamaged]
     * rather than sending any data.
     */
    public val damaged: Boolean

    /**
     * Sends *all* of the data in [buffer].
     */
    public suspend fun sendAll(
        buffer: ByteArray,
        byteCount: UInt = buffer.size.toUInt(),
        bufferOffset: Int = 0
    ): CancellableResult<Unit, Fail>
}

public object StreamDamaged : Fail