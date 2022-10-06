/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.annotations

/**
 * Marker annotation for lower-level APIs that don't have the same safety rails as higher-level
 * APIs. This is different to [Unsafe]; those can cause bad things such as exploits or memory
 * corruption. LowLevelApi marked functions are merely difficult to use.
 *
 * This annotation implies [ProvisionalApi]; all low-level APIs may be deleted at any time without
 * notice unless it is otherwise marked with [StableApi].
 */
@RequiresOptIn(
    message = "This is a low-level API and should have explicit opt-in",
    level = RequiresOptIn.Level.WARNING
)
public annotation class LowLevelApi
