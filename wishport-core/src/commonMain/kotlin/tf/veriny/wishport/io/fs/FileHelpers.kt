/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

// can't wait for multiple receivers

/**
 * Opens a file on the system filesystem and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun FilesystemHandle.Companion.openFile(
    scope: ClosingScope,
    path: SystemPurePath,
    fileOpenMode: FileOpenMode = FileOpenMode.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<SystemFilesystemHandle, Fail> {
    return SystemFilesystem.getFileHandle(path, fileOpenMode, flags)
        .andAddTo(scope)
}

/**
 * Opens a file on the specified [Filesystem], and adds it to the specified [ClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun <Flavour : PurePath<Flavour>> Filesystem<Flavour>.openFile(
    scope: ClosingScope,
    path: Flavour,
    fileOpenMode: FileOpenMode = FileOpenMode.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<FilesystemHandle<Flavour>> {
    return getFileHandle(path, fileOpenMode, flags).andAddTo(scope)
}
