/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.io.Empty

// can't wait for multiple receivers

/**
 * Opens a file on the system filesystem and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun FilesystemHandle.Companion.openFile(
    scope: AsyncClosingScope,
    path: SystemPurePath,
    fileOpenMode: FileOpenMode = FileOpenMode.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<SystemFilesystemHandle> {
    return SystemFilesystem.getFileHandle(path, fileOpenMode, flags)
        .andAddTo(scope)
}

/**
 * Opens a file on the specified [Filesystem], and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun <Flavour : PurePath<Flavour>> Filesystem<Flavour>.openFile(
    scope: AsyncClosingScope,
    path: Flavour,
    fileOpenMode: FileOpenMode = FileOpenMode.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<FilesystemHandle<Flavour>> {
    return getFileHandle(path, fileOpenMode, flags).andAddTo(scope)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory. This will
 * fail with ENOTDIR otherwise.
 */
@Unsafe
public suspend fun <F : PurePath<F>> FilesystemHandle<F>.openRelative(
    path: F,
    mode: FileOpenMode,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<FilesystemHandle<F>, Fail> {
    return filesystem.getRelativeFileHandle(this, path, mode, flags)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory, and then
 * adds it to the specified [scope].
 */
@OptIn(Unsafe::class)
public suspend fun <F : PurePath<F>> FilesystemHandle<F>.openRelative(
    scope: AsyncClosingScope,
    path: F,
    mode: FileOpenMode,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<FilesystemHandle<F>, Fail> {
    return filesystem.getRelativeFileHandle(this, path, mode, flags).andAddTo(scope)
}

/**
 * Flushes the data written into this file to disk. If [withMetadata] is true, then all file
 * metadata will be flushed; otherwise, only essential metadata relating to write consistency
 * will be flushed.
 */
public suspend fun <F : PurePath<F>> FilesystemHandle<F>.flush(
    withMetadata: Boolean = true
): CancellableResult<Empty, Fail> {
    return filesystem.flushFile(this, withMetadata)
}

/**
 * Creates a new directory relative to this filesystem handle.
 */
public suspend fun <F : PurePath<F>> FilesystemHandle<F>.createDirectoryRelative(
    path: F
): CancellableResult<Empty, Fail> {
    return filesystem.mkdirRelative(this, path)
}
