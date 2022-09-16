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

/**
 * Implementation of a closing scope that can be (unsafely) directly constructed.
 */
public class ClosingScopeImpl @Unsafe constructor() : ClosingScope {
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

        if (lastException != null) throw lastException
    }
}
