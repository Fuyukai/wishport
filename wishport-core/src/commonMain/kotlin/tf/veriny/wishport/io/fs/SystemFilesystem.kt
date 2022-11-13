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
import tf.veriny.wishport.core.getIOManager
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
            null, path.toByteString(withNullSep = true), openMode, flags, permissions
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
            otherHandle.raw as DirectoryHandle, path.toByteString(withNullSep = true),
            openMode, flags, permissions
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
        return io.fileMetadataAt(handle?.raw, path.toByteString(withNullSep = true))
    }

    override suspend fun flushFile(
        handle: SysFsHandle,
        withMetadata: Boolean
    ): CancellableResult<Empty, Fail> {
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.fsync(handle.raw, withMetadata = withMetadata)
    }

    override suspend fun createDirectory(
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResourceResult<Empty> {
        return getIOManager().makeDirectoryAt(null, path.toByteString(withNullSep = true), permissions)
    }

    override suspend fun createDirectoryRelative(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResult<Empty, Fail> {
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        return getIOManager().makeDirectoryAt(
            otherHandle.raw as DirectoryHandle,
            path.toByteString(withNullSep = true),
            permissions
        )
    }

    override suspend fun rename(
        fromPath: SystemPurePath,
        toPath: SystemPurePath,
        flags: Set<RenameFlags>
    ): CancellableResourceResult<Empty> {
        return getIOManager().renameAt(
            null, fromPath.toByteString(withNullSep = true),
            null, toPath.toByteString(withNullSep = true),
            flags
        )
    }

    override suspend fun renameRelative(
        fromHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>?,
        fromPath: SystemPurePath,
        toHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>?,
        toPath: SystemPurePath,
        flags: Set<RenameFlags>
    ): CancellableResult<Empty, Fail> {
        if (fromHandle != null) {
            if (fromHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
            if (fromHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
        }
        if (toHandle != null) {
            if (toHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
            if (toHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
        }

        return getIOManager().renameAt(
            fromHandle?.raw as DirectoryHandle?,
            fromPath.toByteString(withNullSep = true),
            toHandle?.raw as DirectoryHandle?,
            toPath.toByteString(withNullSep = true),
            flags
        )
    }

    override suspend fun unlink(
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResourceResult<Empty> {
        val manager = getIOManager()
        return manager.unlinkAt(null, path.toByteString(withNullSep = true), removeDir)
    }

    override suspend fun unlinkRelative(
        otherHandle: SysFsHandle,
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResult<Empty, Fail> {
        if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = getIOManager()
        return manager.unlinkAt(
            otherHandle.raw as DirectoryHandle, path.toByteString(withNullSep = true), removeDir
        )
    }
}
