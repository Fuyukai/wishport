/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe

// can't wait for multiple receivers

/**
 * Opens a buffered file relative to this file if (and only if) this file is a directory.
 */
@Unsafe
@OptIn(ProvisionalApi::class)
public suspend fun SystemFilesystemHandle.openBufferedRelative(
    path: SystemPurePath,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<BufferedFile, Fail> {
    return getFileHandleRelative(path, mode, flags, permissions)
        .andThen { BufferedFile(it as SystemFilesystemHandle) }
}

/**
 * Opens a buffered file relative to this file if (and only if) this file is a directory, and then
 * adds it to the specified [scope].
 */
@OptIn(ProvisionalApi::class)
public suspend fun SystemFilesystemHandle.openBufferedRelative(
    scope: AsyncClosingScope,
    path: SystemPurePath,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE,
): CancellableResult<BufferedFile, Fail> {
    return getFileHandleRelative(scope, path, mode, flags, permissions)
        .andThen { BufferedFile(it as SystemFilesystemHandle) }
}

/**
 * Opens a raw file on the system filesystem and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun FilesystemHandle.Companion.openRawSystemFile(
    scope: AsyncClosingScope,
    path: SystemPurePath,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE,
): CancellableResourceResult<SystemFilesystemHandle> {
    return SystemFilesystem.getFileHandle(path, fileOpenType, flags, permissions)
        .andAddTo(scope)
}

/**
 * Opens a file on the specified [Filesystem], and adds it to the specified [ClosingScope].
 * This returns a raw [FilesystemHandle] that can be used as a reference to an existing path on
 * a filesystem, irrespective of third-party processes changing the file.
 *
 * This is a low-level type that does not implement the Streams API; see [openBufferedSystemFile]
 * and [openUnbufferedFile] for higher-level helpers.
 */
@OptIn(Unsafe::class)
public suspend fun <F : PurePath<F>, M : FileMetadata> Filesystem<F, M>.getFileHandle(
    scope: AsyncClosingScope,
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<FilesystemHandle<F, M>> {
    return getFileHandle(path, fileOpenType, flags, permissions).andAddTo(scope)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory. This will
 * fail with ENOTDIR otherwise.
 */
@Unsafe
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.getFileHandleRelative(
    path: F,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<FilesystemHandle<F, M>, Fail> {
    return filesystem.getFileHandle(this, path, mode, flags, permissions)
}

/**
 * Opens a file relative to this file if (and only if) this file is a directory, and then
 * adds it to the specified [scope].
 */
@OptIn(Unsafe::class)
public suspend fun <F : PurePath<F>, M : FileMetadata> FilesystemHandle<F, M>.getFileHandleRelative(
    scope: AsyncClosingScope,
    path: F,
    mode: FileOpenType,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<FilesystemHandle<F, M>, Fail> {
    return filesystem.getFileHandle(this, path, mode, flags, permissions)
        .andAddTo(scope)
}

/**
 * Opens a new unbuffered file on the specified [Filesystem].
 */
@ProvisionalApi
@Unsafe
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.openUnbufferedFile(
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<UnbufferedFile<F, M>> {
    return getFileHandle(path, fileOpenType, flags, permissions)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
}

/**
 * Opens a new unbuffered file on the specified [Filesystem], adding it to the specified
 * [ClosingScope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.openUnbufferedFile(
    scope: AsyncClosingScope,
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<UnbufferedFile<F, M>> {
    return getFileHandle(scope, path, fileOpenType, flags, permissions)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
}

/**
 * Opens a new [UnbufferedFile] on the specified [Filesystem], relative to the specified [handle].
 */
@Unsafe
@ProvisionalApi
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.openUnbufferedFile(
    handle: FilesystemHandle<F, M>?,
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<UnbufferedFile<F, M>, Fail> {
    return getFileHandle(handle, path, fileOpenType, flags, permissions)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
}

/**
 * Opens a new [UnbufferedFile] on the specified [Filesystem], relative to the specified [handle],
 * and adds it to the specified [scope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.openUnbufferedFile(
    scope: AsyncClosingScope,
    handle: FilesystemHandle<F, M>?,
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<UnbufferedFile<F, M>, Fail> {
    return getFileHandle(handle, path, fileOpenType, flags, permissions)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
        .andAddTo(scope)
}

/**
 * Opens a new unbuffered file on the specified [Filesystem].
 */
@ProvisionalApi
@Unsafe
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.openUnbufferedRelative(
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<UnbufferedFile<F, M>, Fail> {
    return filesystem.openUnbufferedFile(path, fileOpenType, flags, permissions)
}

/**
 * Opens a new unbuffered file on the specified [Filesystem].
 */
@ProvisionalApi
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.openUnbufferedRelative(
    scope: AsyncClosingScope,
    path: F,
    fileOpenType: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<UnbufferedFile<F, M>, Fail> {
    return getFileHandleRelative(scope, path, fileOpenType, flags, permissions)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
}
