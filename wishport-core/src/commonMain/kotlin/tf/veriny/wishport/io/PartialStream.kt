/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * Combines [PartialSendStream] and [Stream] into one object.
 */
@ProvisionalApi
public interface PartialStream : PartialSendStream, Stream
