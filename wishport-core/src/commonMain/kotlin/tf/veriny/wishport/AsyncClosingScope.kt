/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.IdentitySet
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.checkIfCancelled
import tf.veriny.wishport.core.getIOManager
import tf.veriny.wishport.io.FasterCloseable
import tf.veriny.wishport.io.IOHandle

/**
 * Like [ClosingScope], but it supports both [Closeable] and [AsyncCloseable]. This should be
 * preferred over [ClosingScope] in an asynchronous context; the two classes are not compatible.
 *
 * If required, a [ClosingScope] can be added as a child to this [AsyncClosingScope].
 */
public interface AsyncClosingScope : AsyncCloseable {
    public companion object {
        /**
         * Opens a new scope, runs [block] in it, then closes it.
         */
        @OptIn(Unsafe::class)
        public suspend inline operator fun <R : Any> invoke(
            crossinline block: suspend (AsyncClosingScope) -> R
        ): R {
            return AsyncClosingScopeImpl().use(block)
        }
    }

    /**
     * Adds an async closeable to this scope, that will be closed when this scope is also closed.
     */
    public fun add(closeable: AsyncCloseable)

    /**
     * Adds a regular closeable to this scope.
     */
    public fun add(closeable: Closeable)

    /**
     * Removes an async closeable from this scope, stopping it from being tracked.
     *
     * This is useful for avoiding memory leaks with long-lived scopes and objects that are
     * explicitly closed. It is safe to close a Closeable without doing this method, as ``close`` is
     * idempotent.
     */
    public fun remove(closeable: AsyncCloseable)

    /**
     * Removes a regular async closeable from this scope,.
     */
    public fun remove(closeable: Closeable)
}

/**
 * Implementation of a closing scope that can be (unsafely) directly constructed.
 */
public class AsyncClosingScopeImpl @Unsafe constructor() : AsyncClosingScope {
    override var closed: Boolean = false
        private set

    override var closing: Boolean = false
        private set

    // Note: This is an identity set to ensure that two objects which may be equal are both added
    // to the set.
    // This ensures that they both get closed.
    private val closeables = IdentitySet<Closeable>()
    private val toClose = IdentitySet<AsyncCloseable>()

    override fun add(closeable: AsyncCloseable) {
        toClose.add(closeable)
    }

    override fun add(closeable: Closeable) {
        closeables.add(closeable)
    }

    override fun remove(closeable: AsyncCloseable) {
        toClose.add(closeable)
    }

    override fun remove(closeable: Closeable) {
        closeables.remove(closeable)
    }


    @OptIn(LowLevelApi::class)
    override suspend fun close(): CancellableResult<Unit, Fail> {
        if (closing || closed) return Cancellable.empty()
        closing = true

        // open our own shield to protect against improperly written closes
        CancelScope.open(shield = true) {
            closeables.forEach(Closeable::close)

            // separate out the fastercloseables
            val faster = mutableListOf<IOHandle>()

            for (item in toClose) {
                if (item is FasterCloseable) {
                    val handle = item.provideHandleForClosing()
                    if (handle == null) item.close()
                    else faster.add(handle)
                } else {
                    item.close()
                }
            }

            if (faster.isNotEmpty()) {
                val manager = getIOManager()
                manager.closeMany(*faster.toTypedArray())
            }
        }

        closed = true
        return checkIfCancelled()
    }
}

/**
 * Adds the underlying successful result of an [Either] to an [AsyncClosingScope], and returns the
 * Either.
 */
public inline fun <S : AsyncCloseable, F : Fail> Either<S, F>.andAddTo(
    scope: AsyncClosingScope
): Either<S, F> {
    if (isSuccess) scope.add(get()!!)
    return this
}

/**
 * Adds the underlying successful result of an [Cancellable] to an [AsyncClosingScope], and returns
 * the Cancellable.
 */
public inline fun <S : AsyncCloseable, F : Fail> CancellableResult<S, F>.andAddTo(
    scope: AsyncClosingScope
): CancellableResult<S, F> {
    if (isSuccess) scope.add(get()!!)
    return this
}
