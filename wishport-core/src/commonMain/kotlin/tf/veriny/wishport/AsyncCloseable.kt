/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

// see: trio AsyncResource

/**
 * An interface for all objects that are asynchronously closeable.
 */
public interface AsyncCloseable {
    /** Gets if this [AsyncCloseable] is closed. */
    public val closed: Boolean

    /**
     * Closes this object. This is a unique function in that it is the only one that completely
     * ignores cancellation; all cancellations will simply be suppressed until the close operation
     * completes.
     *
     * This function can still return Cancelled, but it does not actually mean the operation was
     * cancelled.
     *
     * This method is idempotent; calling it multiple times has no effect. Subsequent calls will
     * not count as a suspension point.
     */
    public suspend fun close(): CancellableResult<Unit, Fail>
}

public suspend inline fun <S : AsyncCloseable, R> S.use(
    crossinline block: suspend (S) -> R
): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

/**
 * Error object for trying to use an object that's already been closed.
 */
public object ResourceClosed : Fail