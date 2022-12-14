/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.streams

import tf.veriny.wishport.AsyncCloseable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi

// TODO: we need preadv() versions, so we likely need to think about making a Buffer class.

/**
 * The base interface for all objects that can send bytes.
 */
@ProvisionalApi
public interface WriteStream : AsyncCloseable {
    /**
     * Marks this stream as damaged or not.
     *
     * A damaged stream is one that has been cancelled or otherwise failed during [writeAll].
     * Such streams (for example, TLS streams) become essentially useless after this, as it's not
     * easily possible to know what (if any) data was sent or how to retry the operation.
     *
     * If a stream has been damaged, then all attempts at [writeAll] will return [StreamDamaged]
     * rather than sending any data.
     */
    public val damaged: Boolean

    /**
     * Sends *all* of the data in [buffer].
     */
    public suspend fun writeAll(
        buffer: ByteArray,
        byteCount: UInt = buffer.size.toUInt(),
        bufferOffset: Int = 0
    ): CancellableResult<Unit, Fail>
}

public object StreamDamaged : Fail
