/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.AsyncCloseable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.internals.io.ByteCountResult

/**
 * An interface for objects that can asynchronously read byte data.
 */
@ProvisionalApi
public interface ReceiveStream : AsyncCloseable {
    /**
     * Reads up to [byteCount] bytes, but no more. Returns the number of bytes that were
     * actually read.
     */
    public suspend fun readIntoUpto(
        buf: ByteArray,
        byteCount: UInt = buf.size.toUInt(),
        bufferOffset: Int = 0,
    ): CancellableResult<ByteCountResult, Fail>
}
