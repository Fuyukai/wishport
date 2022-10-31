/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.andThen
import tf.veriny.wishport.io.fs.SystemFilesystemHandle

public actual suspend fun SystemFilesystemHandle.getIdealBlockSize(): CancellableResult<UInt, Fail> {
    return filesystem.getFileMetadata(this).andThen { Cancellable.ok(it.blockSize) }
}
