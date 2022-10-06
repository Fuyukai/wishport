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
public suspend fun FileHandle.Companion.openFile(
    scope: ClosingScope,
    path: SystemPurePath,
    fileOpenMode: FileOpenMode = FileOpenMode.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<SystemFileHandle, Fail> {
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
): CancellableResourceResult<FileHandle<Flavour>> {
    return getFileHandle(path, fileOpenMode, flags).andAddTo(scope)
}