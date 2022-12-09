/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.channel

import tf.veriny.wishport.*

/**
 * An object that allows receiving Kotlin objects from connected send channels.
 */
public interface ReceiveChannel<E> : Closeable {
    /**
     * Receives a single object from this channel, without waiting. This will either return a
     * success with the value received, or either a failure with [AlreadyClosedError] if this
     * channel has been closed, or a failure with [ChannelWouldBlock] if there is no data
     * within the channel to receive.
     */
    public fun receiveWithoutWaiting(): Either<E, Fail>

    /**
     * Receives a single object from this channel. This will return an [AlreadyClosedError] if this
     * channel has been closed, or if all of the receiving channels have been closed.
     */
    public suspend fun receive(): CancellableResult<E, AlreadyClosedError>

    /**
     * Creates a clone of this channel. A clone is almost identical to its original forme, but can
     * be closed separately without closing the original.
     */
    public fun clone(): Either<ReceiveChannel<E>, AlreadyClosedError>
}
