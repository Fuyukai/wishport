/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.Empty

// can't wait for multiple receivers

/**
 * Opens a file on the system filesystem and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun FilesystemHandle.Companion.openFile(
    scope: AsyncClosingScope,
    path: SystemPurePath,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<SystemFilesystemHandle> {
    return SystemFilesystem.getFileHandle(path, fileOpenType, flags)
        .andAddTo(scope)
}

/**
 * Opens a file on the specified [Filesystem], and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun <Flavour : PurePath<Flavour>, M : FileMetadata> Filesystem<Flavour, M>.openFile(
    scope: AsyncClosingScope,
    path: Flavour,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<FilesystemHandle<Flavour, M>> {
    return getFileHandle(path, fileOpenType, flags).andAddTo(scope)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory. This will
 * fail with ENOTDIR otherwise.
 */
@Unsafe
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.openRelative(
    path: F,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<FilesystemHandle<F, M>, Fail> {
    return filesystem.getRelativeFileHandle(this, path, mode, flags)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory, and then
 * adds it to the specified [scope].
 */
@OptIn(Unsafe::class)
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.openRelative(
    scope: AsyncClosingScope,
    path: F,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<FilesystemHandle<F, M>, Fail> {
    return filesystem.getRelativeFileHandle(this, path, mode, flags).andAddTo(scope)
}

/**
 * Flushes the data written into this file to disk. If [withMetadata] is true, then all file
 * metadata will be flushed; otherwise, only essential metadata relating to write consistency
 * will be flushed.
 */
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.flush(
    withMetadata: Boolean = true
): CancellableResult<Empty, Fail> {
    return filesystem.flushFile(this, withMetadata)
}

/**
 * Creates a new directory relative to this filesystem handle.
 */
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.createDirectoryRelative(
    path: F
): CancellableResult<Empty, Fail> {
    return filesystem.mkdirRelative(this, path)
}

/**
 * Gets the metadata either for this file,
 */
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.getMetadata(
    path: F? = null
): CancellableResult<M, Fail> {
    return if (path == null) {
        filesystem.getFileMetadata(this)
    } else {
        filesystem.getFileMetadataRelative(this, path)
    }
}
