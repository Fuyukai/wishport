/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.io.fs.SeekWhence

/**
 * Base interface for any I/O-based object that supports seeking.
 */
public interface Seekable {
    /**
     * Seeks this object to the specified [position], with varying behaviour based on the
     * [whence] argument.
     */
    public suspend fun seek(
        position: Long,
        whence: SeekWhence
    ): CancellableResult<SeekPosition, Fail>
}
