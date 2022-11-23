/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.channel

import tf.veriny.wishport.AlreadyClosedError
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.Either

/**
 * An object that allows sending other Kotlin objects to connected receive channels.
 */
public interface SendChannel<E> : Closeable {
    /**
     * Sends a single item down this channel. This will return an [AlreadyClosedError] if this
     * channel has been closed, or if all of the receiving channels have been closed.
     */
    public suspend fun send(item: E): CancellableResult<Unit, AlreadyClosedError>

    /**
     * Creates a clone of this channel. A clone is almost identical to its original forme, but can
     * be closed separately without closing the original.
     */
    public fun clone(): Either<SendChannel<E>, AlreadyClosedError>
}
