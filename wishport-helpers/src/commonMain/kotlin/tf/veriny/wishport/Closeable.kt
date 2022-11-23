/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An object that holds on to external resources, for example a C struct or a file descriptor.
 * Closeables can be automatically registered in a [ClosingScope], which can be opened at the start
 * of a function and will automatically close all [Closeable] objects opened.
 */
public interface Closeable {
    /** If this [Closeable] is closed. */
    public val closed: Boolean

    /**
     * Closes this object. This must not fail.
     */
    public fun close()
}

/**
 * Extension function that runs [block] and then automatically closes this [Closeable].
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return block(this)
    } finally {
        close()
    }
}
