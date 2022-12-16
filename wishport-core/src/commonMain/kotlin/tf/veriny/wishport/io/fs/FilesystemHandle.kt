/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.io.FileLikeHandle
import tf.veriny.wishport.io.Flushable
import tf.veriny.wishport.io.Seekable

/**
 * A handle to an opened file on a filesystem. This can be a real readable or writeable file, or
 * an opened directory.
 *
 * A FilesystemHandle doubles as both a basic file read/write class, and a Path class. All
 * filesystem functions that perform actions on the filesystem structure - such as opening or
 * creating other files or directories - can be passed a FilesystemHandle to open files relative
 * to the real location of this file.
 */
public interface FilesystemHandle<F : PP<F>, M : FM> : FileLikeHandle, Seekable, Flushable {
    public companion object;

    /** The filesystem this handle is open on. */
    public val filesystem: Filesystem<F, M>

    /** The path to this file. */
    public val path: F
}
