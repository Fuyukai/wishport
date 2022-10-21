package tf.veriny.wishport.io.streams

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * Like a [ReadStream], but with explicit buffer logic.
 */
@ProvisionalApi
public interface BufferedReadStream : ReadStream {
    /**
     * Attempts a read from the buffered storage for this stream, returning the number of bytes
     * actually read.
     *
     * This will not perform a system call; if there is no data to read this will simply return
     * zero. Additionally, if the parameters are invalid, it will also return zero. Use
     * [readIntoUpto] for actually reading from the underlying source.
     */
    public fun readFromBuffer(
        into: ByteArray, byteCount: Int = into.size, bufferOffset: Int = 0
    ): Int
}