/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.AsyncCloseable

// gross interfaces, all in the name of performance!

/**
 * A closeable that allows for batching close requests internally to avoid extra suspension.
 */
public interface FasterCloseable : AsyncCloseable {
    /**
     * Provides the underlying [IOHandle] that this [FasterCloseable] wraps. This should
     * also set the [closing] variable.
     *
     * This may return null for wrapper types that wish to implement FasterCloseable, but are
     * unsure if what they wrap implements FasterCloseable. See ``UnbufferedFile`` for an example.
     */
    public fun provideHandleForClosing(): IOHandle?

    /**
     * Notifies this [FasterCloseable] that the underlying handle has been closed elsewhere.
     */
    public fun notifyClosed()
}
