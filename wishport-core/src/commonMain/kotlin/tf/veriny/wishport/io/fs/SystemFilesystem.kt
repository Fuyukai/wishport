/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.io.DirectoryHandle
import tf.veriny.wishport.io.Empty

private typealias SysFsHandle = FilesystemHandle<SystemPurePath, PlatformFileMetadata>

/**
 * The filesystem for the main operating system namespace.
 */
@OptIn(LowLevelApi::class)
public object SystemFilesystem : Filesystem<SystemPurePath, PlatformFileMetadata> {
    override val currentDirectoryPath: SystemPurePath = systemPathFor(b(".")).get()!!

    @Unsafe
    override suspend fun getFileHandle(
        path: SystemPurePath,
        openMode: FileOpenType,
        flags: Set<FileOpenFlags>,
        permissions: Set<FilePermissions>
    ): CancellableResourceResult<SystemFilesystemHandle> {
        val manager = getIOManager()
        return manager.openFilesystemFile(
            null, path.toByteString(), openMode, flags, permissions
        )
            .andThen {
                Cancellable.ok(SystemFilesystemHandle(this, it, path))
            }
    }

    @Unsafe
    override suspend fun getRelativeFileHandle(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        openMode: FileOpenType,
        flags: Set<FileOpenFlags>,
        permissions: Set<FilePermissions>
    ): CancellableResult<SystemFilesystemHandle, Fail> {
        // NB: on linux this will still go to io_uring due to DirectoryHandle and FileHandle being
        // identical (and then return the error), but on Windows it'll fail.
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.openFilesystemFile(
            otherHandle.raw as DirectoryHandle, path.toByteString(), openMode, flags, permissions
        )
            .andThen {
                Cancellable.ok(SystemFilesystemHandle(this, it, path))
            }
    }

    override suspend fun getFileMetadata(
        handle: SysFsHandle
    ): CancellableResult<PlatformFileMetadata, Fail> {
        val io = getIOManager()
        return io.fileMetadataAt(handle.raw, null)
    }

    override suspend fun getFileMetadataRelative(
        handle: SysFsHandle?,
        path: SystemPurePath
    ): CancellableResult<PlatformFileMetadata, Fail> {
        val io = getIOManager()
        return io.fileMetadataAt(handle?.raw, path.toByteString())
    }

    override suspend fun flushFile(
        handle: SysFsHandle,
        withMetadata: Boolean
    ): CancellableResult<Empty, Fail> {
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.fsync(handle.raw, withMetadata = withMetadata)
    }

    override suspend fun mkdir(
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResourceResult<Empty> {
        return getIOManager().makeDirectoryAt(null, path.toByteString(), permissions)
    }

    override suspend fun mkdirRelative(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResult<Empty, Fail> {
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        return getIOManager().makeDirectoryAt(
            otherHandle.raw as DirectoryHandle,
            path.toByteString(),
            permissions
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
