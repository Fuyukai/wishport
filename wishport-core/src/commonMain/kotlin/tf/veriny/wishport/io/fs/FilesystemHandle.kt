/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.io.Empty
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

/**
 * A handle to an opened file on a filesystem.
 */
public interface FilesystemHandle<F : PurePath<F>> : Closeable, FileLikeHandle {
    public companion object;

    /** The filesystem this handle is open on. */
    public val filesystem: Filesystem<F>

    /** The path to this file. */
    public val path: F

    /**
     * Opens a file relative to this file if (and only if) this file is a directory. This will
     * fail with ENOTDIR otherwise.
     */
    @Unsafe
    public suspend fun openRelative(
        path: F,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResult<FilesystemHandle<F>, Fail>

    /**
     * Flushes the data written into this file to disk. If [withMetadata] is true, then all file
     * metadata will be flushed; otherwise, only essential metadata relating to write consistency
     * will be flushed.
     */
    public suspend fun flush(withMetadata: Boolean = true): CancellableResourceResult<Empty>
}
