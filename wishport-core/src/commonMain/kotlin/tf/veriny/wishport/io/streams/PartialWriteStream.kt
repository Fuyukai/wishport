/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.streams

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.io.ByteCountResult

/**
 * Like [WriteStream], but allows partial sending.
 */
@ProvisionalApi
public interface PartialWriteStream : WriteStream {
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
     *
     * If this method encounters an error, but data has already been sent, it will return a
     * [WriteMostFailed] that wraps the original error as well as the total number of bytes that
     * was written before the error occurred. Otherwise, it will just directly return the error.
     */
    public suspend fun writeMost(
        buffer: ByteArray,
        byteCount: UInt = buffer.size.toUInt(),
        bufferOffset: Int = 0
    ): CancellableResult<ByteCountResult, Fail>
}

/**
 * Returned when [PartialWriteStream.writeMost] fails.
 */
public class WriteMostFailed(public val why: Fail, public val byteCount: Int) : Fail
