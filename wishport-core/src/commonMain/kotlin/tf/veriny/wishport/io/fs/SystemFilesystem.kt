package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.io.DirectoryHandle
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

/**
 * The filesystem for the main operating system namespace.
 */
@OptIn(LowLevelApi::class)
public object SystemFilesystem : Filesystem<SystemPurePath> {
    @Unsafe
    override suspend fun getFileHandle(
        path: SystemPurePath, openMode: FileOpenMode, flags: Set<FileOpenFlags>
    ): CancellableResourceResult<SystemFileHandle> {
        val manager = EventLoop.get().ioManager
        return manager.openFilesystemFile(
            null, path.toByteString(), openMode, flags
        )
            .andThen {
                return Cancellable.ok(SystemFileHandle(this, it, path))
            }
    }

    @Unsafe
    override suspend fun getRelativeFileHandle(
        handle: FileHandle<SystemPurePath>,
        path: SystemPurePath,
        openMode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResult<SystemFileHandle, Fail> {
        // NB: on linux this will still go to io_uring due to DirectoryHandle and FileHandle being
        // identical (and then return the error), but on Windows it'll fail.
        if (handle.raw !is DirectoryHandle) return Cancellable.failed(NotADirectory)
        if (handle.filesystem != this) return Cancellable.failed(WrongFilesystemError)

        val manager = EventLoop.get().ioManager
        return manager.openFilesystemFile(
            handle.raw as DirectoryHandle, path.toByteString(), openMode, flags
        )
            .andThen {
                return Cancellable.ok(SystemFileHandle(this, it, path))
            }
    }
}