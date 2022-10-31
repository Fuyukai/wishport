/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.streams

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * A combination of the [Stream] and [BufferedReadStream] API.
 */
@ProvisionalApi
public interface BufferedStream : Stream, BufferedReadStream
