package tf.veriny.wishport.io

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.internals.io.ByteCountResult

/**
 * Like [SendStream], but allows partial sending.
 */
@ProvisionalApi
public interface PartialSendStream : SendStream {
    /**
     * Attempts to send most (if not all) data over this stream. This follows a slightly different
     * but relatively familiar set of cancellation semantics:
     *
     * 1. If the operation is cancelled before any data is sent, this will just return Cancelled.
     * 2. If the operation is cancelled before all the data is sent, this will return a
     *    [ByteCountResult] that can be used to re-try the sending if required.
     * 3. Otherwise, this method will send all of the data and return [byteCount].
     *
     * This interface should only be implemented by streams that do support cancellation in
     * the middle of sending a message. Streams with complex framing requirements (e.g. TLS or
     * websockets) must not implement this.
     */
    public suspend fun sendMost(
        buffer: ByteArray,
        byteCount: UInt = buffer.size.toUInt(),
        bufferOffset: Int = 0
    ): CancellableResult<ByteCountResult, Fail>
}