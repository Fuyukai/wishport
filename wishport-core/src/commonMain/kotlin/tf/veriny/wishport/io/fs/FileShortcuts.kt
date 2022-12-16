/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.Empty

// this used to be one file but now it's three.
// this file contains helper extensions that are shortcuts for doing ``filesystem.X(handle)``
// file openers were split into FileOpeners.kt
// and extra extension functions that aren't specific to a filesystem were split into FsExt.

// == Filesystem Handle ops == //
/**
 * Creates a new directory relative to this filesystem handle.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.createDirectoryRelative(
    path: F,
    permissions: Set<FilePermissions> = setOf()
): CancellableResult<Empty, Fail> {
    return filesystem.createDirectory(this, path, permissions)
}

/**
 * Unlinks a file or symbolic link relative to this filesystem handle.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.unlinkRelative(
    path: F
): CancellableResult<Empty, Fail> {
    return filesystem.unlink(this, path, removeDir = false)
}

/**
 * Removes an empty directory relative to this filesystem handle.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.removeDirectoryRelative(
    path: F
): CancellableResult<Empty, Fail> {
    return filesystem.unlink(this, path, removeDir = true)
}

/**
 * Gets the metadata either for this file, or the file at [path] relative to this open file.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.metadataFor(
    path: F? = null
): CancellableResourceResult<M> {
    @Suppress("UNCHECKED_CAST")
    return if (path == null) {
        filesystem.getFileMetadata(this)
    } else {
        filesystem.getFileMetadata(this, path)
    } as CancellableResourceResult<M>
}

/**
 * Lists the contents for the directory at [path].
 */
@OptIn(Unsafe::class)
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.listDirectory(
    path: F
): CancellableResult<List<DirectoryEntry>, Fail> = AsyncClosingScope { scope ->
    getFileHandle(
        path, FileOpenType.READ_ONLY, setOf(FileOpenFlags.DIRECTORY)
    )
        .andAddTo(scope)
        .andThen { it.listDirectory() }
}

/**
 * Lists the directory contents for the specified open directory.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.listDirectory(): CancellableResult<List<DirectoryEntry>, Fail> {
    return filesystem.getDirectoryEntries(this)
}
