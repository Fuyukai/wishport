/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.annotations

/**
 * Marker annotation for APIs that are provisional and subject to breaking changes and/or removal.
 */
@RequiresOptIn(
    message = "This API is subject to change and requires opt-in",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
public annotation class ProvisionalApi
