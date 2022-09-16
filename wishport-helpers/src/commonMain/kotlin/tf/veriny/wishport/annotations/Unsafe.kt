/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.annotations

/**
 * Marker annotation for unsafe (i.e. leaky) operations.
 */
@RequiresOptIn(message = "This operation is unsafe and requires explicit opt-in")
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Unsafe
