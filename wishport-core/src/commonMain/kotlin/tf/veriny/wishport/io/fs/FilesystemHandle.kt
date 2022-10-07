/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * A handle to an opened file on a filesystem.
 */
public interface FilesystemHandle<F : PurePath<F>> : FileLikeHandle {
    public companion object;

    /** The filesystem this handle is open on. */
    public val filesystem: Filesystem<F>

    /** The path to this file. */
    public val path: F
}
