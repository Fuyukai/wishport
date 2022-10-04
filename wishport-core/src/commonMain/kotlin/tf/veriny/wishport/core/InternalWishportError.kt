/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

/**
 * Thrown when something is deeply, deeply wrong in Wishport's core. Do not attempt to catch this.
 */
public class InternalWishportError(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)
