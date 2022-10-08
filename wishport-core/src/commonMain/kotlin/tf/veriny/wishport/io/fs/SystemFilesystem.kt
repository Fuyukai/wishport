/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.io.DirectoryHandle
import tf.veriny.wishport.internals.io.Empty
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

private typealias SysFsHandle = FilesystemHandle<SystemPurePath>

/**
 * The filesystem for the main operating system namespace.
 */
@OptIn(LowLevelApi::class)
public object SystemFilesystem : Filesystem<SystemPurePath> {
    @Unsafe
    override suspend fun getFileHandle(
        path: SystemPurePath,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResourceResult<SystemFilesystemHandle> {
        val manager = getIOManager()
        return manager.openFilesystemFile(
            null, path.toByteString(), openMode, flags
        )
            .andThen {
                return Cancellable.ok(SystemFilesystemHandle(this, it, path))
            }
    }

    @Unsafe
    override suspend fun getRelativeFileHandle(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResult<SystemFilesystemHandle, Fail> {
        // NB: on linux this will still go to io_uring due to DirectoryHandle and FileHandle being
        // identical (and then return the error), but on Windows it'll fail.
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.openFilesystemFile(
            otherHandle.raw as DirectoryHandle, path.toByteString(), openMode, flags
        )
            .andThen {
                return Cancellable.ok(SystemFilesystemHandle(this, it, path))
            }
    }

    override suspend fun flushFile(
        handle: FilesystemHandle<SystemPurePath>,
        withMetadata: Boolean
    ): CancellableResult<Empty, Fail> {
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.fsync(handle.raw, withMetadata = withMetadata)
    }

    override suspend fun mkdir(path: SystemPurePath): CancellableResourceResult<Empty> {
        return getIOManager().makeDirectoryAt(null, path.toByteString())
    }

    override suspend fun mkdirRelative(
        otherHandle: FilesystemHandle<SystemPurePath>,
        path: SystemPurePath
    ): CancellableResult<Empty, Fail> {
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        return getIOManager().makeDirectoryAt(
            otherHandle.raw as DirectoryHandle,
            path.toByteString()
        )
    }

    override suspend fun unlink(
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResourceResult<Empty> {
        val manager = getIOManager()
        return manager.unlinkAt(null, path.toByteString(), removeDir)
    }

    override suspend fun unlinkRelative(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResult<Empty, Fail> {
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.unlinkAt(otherHandle.raw as DirectoryHandle, path.toByteString(), removeDir)
    }
}
