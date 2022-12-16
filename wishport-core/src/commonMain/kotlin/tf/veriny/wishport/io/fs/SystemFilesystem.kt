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
        return getFileHandle(null, path, openMode, flags, permissions)
            as CancellableResourceResult<SystemFilesystemHandle>
    }

    @Unsafe
    override suspend fun getFileHandle(
        otherHandle: SysFsHandle?,
        path: SystemPurePath,
        openMode: FileOpenType,
        flags: Set<FileOpenFlags>,
        permissions: Set<FilePermissions>
    ): CancellableResult<SystemFilesystemHandle, Fail> {
        // NB: on linux this will still go to io_uring due to DirectoryHandle and FileHandle being
        // identical (and then return the error), but on Windows it'll fail.
        if (otherHandle != null) {
            if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
            if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
        }

        val manager = getIOManager()
        return manager.openFilesystemFile(
            otherHandle?.raw as DirectoryHandle?, path.toByteString(withNullSep = true),
            openMode, flags, permissions
        )
            .andThen {
                Cancellable.ok(SystemFilesystemHandle(this, it, path))
            }
    }

    override suspend fun getFileMetadata(
        handle: SysFsHandle
    ): CancellableResult<PlatformFileMetadata, Fail> {
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val io = getIOManager()
        return io.fileMetadataAt(handle.raw, null)
    }

    override suspend fun getFileMetadata(
        handle: SysFsHandle?,
        path: SystemPurePath
    ): CancellableResult<PlatformFileMetadata, Fail> {
        if (handle != null && handle.filesystem != this) {
            return Cancellable.failed(WrongFilesystemError)
        }

        val io = getIOManager()
        return io.fileMetadataAt(handle?.raw, path.toByteString(withNullSep = true))
    }

    override suspend fun getDirectoryEntries(
        handle: SysFsHandle
    ): CancellableResult<List<DirectoryEntry>, Fail> {
        if (handle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val io = getIOManager()
        return io.getDirectoryEntries(handle.raw)
    }

    override suspend fun createDirectory(
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResourceResult<Empty> {
        return getIOManager().makeDirectoryAt(null, path.toByteString(withNullSep = true), permissions)
    }

    override suspend fun createDirectory(
        otherHandle: SysFsHandle?,
        path: SystemPurePath,
        permissions: Set<FilePermissions>
    ): CancellableResult<Empty, Fail> {
        if (otherHandle != null) {
            if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
            if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
        }

        return getIOManager().makeDirectoryAt(
            otherHandle?.raw as DirectoryHandle?,
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

    override suspend fun rename(
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

    override suspend fun hardlink(
        existingFile: SystemPurePath,
        newFile: SystemPurePath
    ): CancellableResourceResult<Empty> {
        return hardlink(null, existingFile, null, newFile)
            as CancellableResourceResult<Empty>
    }

    override suspend fun hardlink(
        existingHandle: SysFsHandle?,
        existingPath: SystemPurePath,
        newHandle: SysFsHandle?,
        newPath: SystemPurePath
    ): CancellableResult<Empty, Fail> {
        if (existingHandle != null && existingHandle.filesystem != this)
            return Cancellable.failed(WrongFilesystemError)
        if (newHandle != null && newHandle.filesystem != this)
            return Cancellable.failed(WrongFilesystemError)

        val io = getIOManager()
        val path1 = existingPath.toByteString(withNullSep = true)
        val path2 = newPath.toByteString(withNullSep = true)
        return io.linkAt(
            existingHandle?.raw as DirectoryHandle,
            path1,
            newHandle?.raw as DirectoryHandle,
            path2
        )
    }

    override suspend fun symbolicLink(
        targetPath: SystemPurePath,
        newPath: SystemPurePath
    ): CancellableResourceResult<Empty> {
        val io = getIOManager()
        return io.symlinkAt(
            targetPath.toByteString(withNullSep = true),
            null,
            newPath.toByteString(withNullSep = true),
        )
    }

    override suspend fun symbolicLink(
        targetPath: SystemPurePath,
        newHandle: SysFsHandle?,
        newPath: SystemPurePath
    ): CancellableResult<Empty, Fail> {
        if (newHandle != null) {
            if (newHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
            if (newHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        }

        val io = getIOManager()
        return io.symlinkAt(
            targetPath.toByteString(withNullSep = true),
            newHandle?.raw as DirectoryHandle?,
            newPath.toByteString(withNullSep = true),
        )
    }

    override suspend fun unlink(
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResourceResult<Empty> {
        val manager = getIOManager()
        return manager.unlinkAt(null, path.toByteString(withNullSep = true), removeDir)
    }

    override suspend fun unlink(
        otherHandle: SysFsHandle?,
        path: SystemPurePath,
        removeDir: Boolean
    ): CancellableResult<Empty, Fail> {
        if (otherHandle != null) {
            if (otherHandle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
            if (otherHandle.filesystem != this) return Cancellable.failed(WrongFilesystemError)
        }

        val manager = getIOManager()
        return manager.unlinkAt(
            otherHandle?.raw as DirectoryHandle?, path.toByteString(withNullSep = true), removeDir
        )
    }
}
