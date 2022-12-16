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
import tf.veriny.wishport.io.Empty

// see: java.nio.file.Filesystem
// naming convention:
// filesystem uses overloads for relatives, e.g. doX(path) and doX(relative_handle, path)
// the relative overloads should take null for better API composability with other functions that
// conditionally take a relative handle.

internal typealias PP<F> = PurePath<F>
internal typealias FM = FileMetadata

/**
 * A Filesystem is a way of interacting with a storage mechanism for individual files indexed by
 * a path.
 *
 * A Wishport filesystem is independent of the idea of real, operating system level filesystems;
 * one filesystem covers the entire OS file namespace. Filesystems can be created over any archive
 * or remote access system, such as a zip file or FTP.
 */
public interface Filesystem<Flavour : PurePath<Flavour>, Metadata : FileMetadata> {
    public companion object;

    /**
     * The [PurePath] that corresponds to the current directory of the running processes in the
     * flavour that this filesystem expects.
     */
    public val currentDirectoryPath: Flavour

    /**
     * Opens a new file handle for this filesystem, at [path].
     */
    @Unsafe
    public suspend fun getFileHandle(
        path: Flavour,
        openMode: FileOpenType,
        flags: Set<FileOpenFlags> = setOf(),
        // only relevant on files, so this is fine even for directories.
        permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE,
    ): CancellableResourceResult<FilesystemHandle<Flavour, Metadata>>

    /**
     * Gets a relative file handle from the specified [otherHandle].
     */
    @Unsafe
    public suspend fun getFileHandle(
        otherHandle: FilesystemHandle<Flavour, Metadata>?,
        path: Flavour,
        openMode: FileOpenType,
        flags: Set<FileOpenFlags> = setOf(),
        permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
    ): CancellableResult<FilesystemHandle<Flavour, Metadata>, Fail>

    /**
     * Gets metadata about the file at [handle].
     */
    public suspend fun getFileMetadata(
        handle: FilesystemHandle<Flavour, Metadata>,
    ): CancellableResult<Metadata, Fail>

    /**
     * Gets metadata about the file at [path].
     *
     * If [handle] is not null then [handle] should refer to an open directory,
     * and [path] should refer to a relative file in that directory; the metadata will be for the
     * file at the path provided.
     *
     * If [handle] is null, or [path] is an absolute path regardless of the
     * status of [handle], then the metadata will be for the file at the absolute path provided, or
     * the file at the path relative to the current directory if [path] is a relative path.
     */
    public suspend fun getFileMetadata(
        handle: FilesystemHandle<Flavour, Metadata>?,
        path: Flavour
    ): CancellableResult<Metadata, Fail>

    /**
     * Gets a list of [DirectoryEntry] instances for the specified open directory.
     */
    public suspend fun getDirectoryEntries(
        handle: FilesystemHandle<Flavour, Metadata>
    ): CancellableResult<List<DirectoryEntry>, Fail>

    /**
     * Creates a new, empty directory at [path].
     */
    public suspend fun createDirectory(
        path: Flavour,
        permissions: Set<FilePermissions> = FilePermissions.DEFAULT_DIRECTORY
    ): CancellableResourceResult<Empty>

    /**
     * Creates a new, empty directory at the [path] relative to the directory at [otherHandle].
     */
    public suspend fun createDirectory(
        otherHandle: FilesystemHandle<Flavour, Metadata>?,
        path: Flavour,
        permissions: Set<FilePermissions> = FilePermissions.DEFAULT_DIRECTORY
    ): CancellableResult<Empty, Fail>

    /**
     * Renames the file or directory at [fromPath] to [toPath].
     */
    public suspend fun rename(
        fromPath: Flavour,
        toPath: Flavour,
        flags: Set<RenameFlags> = setOf()
    ): CancellableResourceResult<Empty>

    /**
     * Renames the file or directory at the path [fromPath] relative to [fromHandle] to the path
     * [toPath] relative to [toHandle]. [fromHandle] or [toHandle] can be null; if both are null,
     * this method acts identically to [rename].
     */
    public suspend fun rename(
        fromHandle: FilesystemHandle<Flavour, Metadata>?,
        fromPath: Flavour,
        toHandle: FilesystemHandle<Flavour, Metadata>?,
        toPath: Flavour,
        flags: Set<RenameFlags> = setOf()
    ): CancellableResult<Empty, Fail>

    /**
     * Creates a new hardlink to the file referred to by [existingFile], at the path [newFile].
     */
    public suspend fun hardlink(
        existingFile: Flavour,
        newFile: Flavour
    ): CancellableResourceResult<Empty>

    /**
     * Creates a new hardlink to the file referred to by [existingPath], relative to
     * [existingHandle], at the path [newPath], relative to [newHandle].
     *
     * Either [existingHandle] or [newHandle] can be null; if both are null, this method will
     * behave identically to the ordinary [hardlink] method.
     */
    public suspend fun hardlink(
        existingHandle: FilesystemHandle<Flavour, Metadata>?,
        existingPath: Flavour,
        newHandle: FilesystemHandle<Flavour, Metadata>?,
        newPath: Flavour,
    ): CancellableResult<Empty, Fail>

    /**
     * Creates a new symbolic link with the content [targetPath], at the path [newPath].
     */
    public suspend fun symbolicLink(
        targetPath: Flavour,
        newPath: Flavour
    ): CancellableResourceResult<Empty>

    /**
     * Creates a new symbolic link with the content [targetPath], at the path [newPath] that is
     * relative to the specified [newHandle].
     */
    public suspend fun symbolicLink(
        targetPath: Flavour,
        newHandle: FilesystemHandle<Flavour, Metadata>?,
        newPath: Flavour,
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
    public suspend fun unlink(
        otherHandle: FilesystemHandle<Flavour, Metadata>?,
        path: Flavour,
        removeDir: Boolean = false
    ): CancellableResult<Empty, Fail>
}

// workaround for lack of Self types, otherwise FileHandle would be FileHandle<Flavour, Filesystem>.
/**
 * Returned when a filesystem method is invoked with a file handle from the wrong filesystem.
 */
public object WrongFilesystemError : Fail
