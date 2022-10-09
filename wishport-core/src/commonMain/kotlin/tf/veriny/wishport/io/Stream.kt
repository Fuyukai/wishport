/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * Combines [ReceiveStream] and [SendStream] into one object.
 */
@ProvisionalApi
public interface Stream : ReceiveStream, SendStream {
    /**
     * Sends an EOF marker over this stream. This will tell the other side of the stream that it
     * should no longer expect any data to be written (e.g. server-side ``recv()`` will return zero
     * bytes read).
     */
    public suspend fun sendEof(): CancellableResult<Unit, Fail>
}

/**
 * Returned from [Stream.sendEof] if it is not supported.
 */
public object EofNotSupported : Fail
