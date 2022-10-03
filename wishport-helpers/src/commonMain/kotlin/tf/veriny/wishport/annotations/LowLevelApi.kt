/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.annotations

/**
 * Marker annotation for lower-level APIs
 */
@RequiresOptIn(message = "This is a low-level API and should have explicit opt-in", level = RequiresOptIn.Level.WARNING)
public annotation class LowLevelApi
