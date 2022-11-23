/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

// side note: anything that impls these operators counts as an iterator according to kotlin
// these are just helpers for extension functions mostly

public interface AsyncIterable<out E> {
    public suspend operator fun iterator(): AsyncIterator<E>
}

public interface AsyncIterator<out E> {
    public suspend operator fun hasNext(): Boolean

    public suspend operator fun next(): E
}
