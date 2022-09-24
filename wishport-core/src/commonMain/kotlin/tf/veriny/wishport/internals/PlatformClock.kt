/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Clock

/**
 * A clock that uses the current computer's idea of time.
 */
@LowLevelApi
public expect object PlatformClock : Clock
