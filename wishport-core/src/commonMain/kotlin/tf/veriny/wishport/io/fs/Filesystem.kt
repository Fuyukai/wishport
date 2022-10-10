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
import tf.veriny.wishport.internals.io.Empty

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
     * Gets a relative file handle from the specified [otherHandle].
     */
    @Unsafe
    public suspend fun getRelativeFileHandle(
        otherHandle: FilesystemHandle<Flavour>,
        path: Flavour,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags> = setOf(),
    ): CancellableResult<FilesystemHandle<Flavour>, Fail>

    /**
     * Flushes the data written into the specified file to disk. If [withMetadata] is true, then all file
     * metadata will be flushed; otherwise, only essential metadata relating to write consistency
     * will be flushed.
     */
    public suspend fun flushFile(
        handle: FilesystemHandle<Flavour>,
        withMetadata: Boolean = true
    ): CancellableResult<Empty, Fail>

    /**
     * Creates a new, empty directory at [path].
     */
    public suspend fun mkdir(path: Flavour): CancellableResourceResult<Empty>

    /**
     * Creates a new, empty directory at the [path] relative to the directory at [otherHandle].
     */
    public suspend fun mkdirRelative(
        otherHandle: FilesystemHandle<Flavour>,
        path: Flavour
    ): CancellableResult<Empty, Fail>

    /**
     * Unlinks a file from this filesystem. If [removeDir] is true, and the path is an empty
     * directory, then the directory will be removed; otherwise, this will fail with EISDIR.
     */
    public suspend fun unlink(
        path: Flavour,
        removeDir: Boolean = false
    ): CancellableResourceResult<Empty>

    /**
     * Unlinks a file from this filesystem, relative to [otherHandle].
     */
    public suspend fun unlinkRelative(
        otherHandle: FilesystemHandle<Flavour>,
        path: Flavour,
        removeDir: Boolean = false
    ): CancellableResult<Empty, Fail>
}

// workaround for lack of Self types, otherwise FileHandle would be FileHandle<Flavour, Filesystem>.
/**
 * Returned when a filesystem method is invoked with a file handle from the wrong filesystem.
 */
public object WrongFilesystemError : Fail
