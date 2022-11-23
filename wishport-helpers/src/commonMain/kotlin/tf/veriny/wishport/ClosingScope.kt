/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.IdentitySet

/**
 * A scope that can have objects added to it to automatically close them.
 *
 * This is an interface so that classes can easily inherit (and delegate) to it.
 */
public interface ClosingScope : Closeable {
    public companion object {
        /**
         * Opens a new scope, runs [block] in it, then closes it.
         */
        @OptIn(Unsafe::class)
        public inline operator fun <R : Any> invoke(block: (ClosingScope) -> R): R {
            return ClosingScopeImpl().use(block)
        }
    }

    /**
     * Adds a closeable to this scope, that will be closed when this scope is also closed.
     */
    public fun add(closeable: Closeable)

    /**
     * Removes a closeable from this scope, stopping it from being tracked.
     *
     * This is useful for avoiding memory leaks with long-lived scopes and objects that are
     * explicitly closed. It is safe to close a Closeable without doing this method, as ``close`` is
     * idempotent.
     */
    public fun remove(closeable: Closeable)
}

// ideally you'd delegate with `constructor (scope: ClosingScope) : ClosingScope by scope`
// but if ur really stupid ur gonna make a ClosingScope impl yourself so this is there to avoid
// footgunning w/ IdentitySet.

/**
 * Implementation of a closing scope that can be (unsafely) directly constructed.
 */
public class ClosingScopeImpl @Unsafe constructor() : ClosingScope {
    override var closed: Boolean = false
        private set

    // Note: This is an identity set to ensure that two objects which may be equal are both added
    // to the set.
    // This ensures that they both get closed.
    private val toClose = IdentitySet<Closeable>()

    override fun add(closeable: Closeable) {
        toClose.add(closeable)
    }

    override fun remove(closeable: Closeable) {
        toClose.remove(closeable)
    }

    override fun close() {
        var lastException: Throwable? = null
        for (item in toClose) {
            try {
                item.close()
            } catch (e: Throwable) {
                lastException = e
            }
        }

        closed = true
        if (lastException != null) throw lastException
    }
}

/**
 * Adds the underlying successful result of an [Either] to this [ClosingScope], and returns the
 * Either.
 */
public inline fun <S : Closeable, F : Fail> Either<S, F>.andAddTo(
    scope: ClosingScope
): Either<S, F> {
    if (isSuccess) scope.add(get()!!)
    return this
}

/**
 * Adds the underlying successful result of an [Cancellable] to this [ClosingScope], and returns the
 * Cancellable.
 */
public inline fun <S : Closeable, F : Fail> CancellableResult<S, F>.andAddTo(
    scope: ClosingScope
): CancellableResult<S, F> {
    if (isSuccess) scope.add(get()!!)
    return this
}
