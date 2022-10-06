/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

// see: java.nio.file.Filesystem

/**
 * A Filesystem is a way of interacting with a storage mechanism for individual files indexed by
 * a path.
 *
 * A Wishport filesystem is independent of the idea of real, operating system level filesystems;
 * one filesystem covers the entire OS file namespace. Filesystems can be created over any archive
 * or remote access system, such as a zip file or FTP.
 */
public interface Filesystem<Flavour : PurePath<Flavour>> {
    public companion object;

    /**
     * Opens a new file handle for this filesystem.
     */
    @Unsafe
    public suspend fun getFileHandle(
        path: Flavour,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags> = setOf(),
    ): CancellableResourceResult<FilesystemHandle<Flavour>>

    /**
     * Gets a relative file handle from the specified [handle].
     */
    @Unsafe
    public suspend fun getRelativeFileHandle(
        handle: FilesystemHandle<Flavour>,
        path: Flavour,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags> = setOf(),
    ): CancellableResult<FilesystemHandle<Flavour>, Fail>
}

// workaround for lack of Self types, otherwise FileHandle would be FileHandle<Flavour, Filesystem>.
/**
 * Returned when a filesystem method is invoked with a file handle from the wrong filesystem.
 */
public object WrongFilesystemError : Fail
