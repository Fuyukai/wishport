package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.decodeToString
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.checkIfCancelled
import tf.veriny.wishport.io.Empty

/**
 * Removes a file or empty directory at the specified location.
 */
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.remove(
    path: F
): CancellableResourceResult<Empty> {
    return unlink(path).combinate(
        { Cancellable.ok(it) },
        { unlink(path, removeDir = true) }
    )
}

/**
 * Removes a file, symbolic link, or empty directory relative to this filesystem handle.
 */
public suspend fun <F : PP<F>, M : FM> FilesystemHandle<F, M>.removeRelative(
    path: F
): CancellableResult<Empty, Fail> {
    return unlinkRelative(path).combinate(
        { Cancellable.ok(it) },
        { removeDirectoryRelative(path) }
    )
}

// == Read-write helpers == //

@OptIn(ProvisionalApi::class)
private suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.doAtomicWrite(
    handle: FilesystemHandle<F, M>?,
    scope: AsyncClosingScope,
    original: F,
    outPath: F,
    content: ByteArray,
    byteCount: UInt,
    bufferOffset: Int,
    permissions: Set<FilePermissions>
): CancellableResourceResult<Unit> = CancelScope(shield = true) {
    val flags = setOf(FileOpenFlags.MUST_CREATE, FileOpenFlags.NO_ACCESS_TIME)

    openUnbufferedFile(scope, handle, outPath, FileOpenType.WRITE_ONLY, flags, permissions)
        .andAlso { it.writeAll(content, byteCount, bufferOffset) }
        // TODO: add some sort of io_uring fused write-flush-close op?
        .andAlso { it.flush() }
        .andAlso { it.close() }
        .andThen { rename(handle, outPath, handle, original) }
        .andThen { Cancellable.empty() } as CancellableResourceResult<Unit>
}

@OptIn(ProvisionalApi::class)
private suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.doWriteAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    content: ByteArray,
    byteCount: UInt,
    bufferOffset: Int,
    atomic: Boolean,
    flags: Set<FileOpenFlags>,
    permissions: Set<FilePermissions>,
): CancellableResult<Unit, Fail> {
    return if (atomic) {
        val name = path.fileName ?: return Cancellable.failed(IsADirectory)
        val genned = SecureRandom.randomByteString(16)
        val newName = name + genned
        val newPath = path.withName(newName).expect("should have succeeded")

        AsyncClosingScope { scope ->
            doAtomicWrite(
                handle,
                scope,
                original = path,
                outPath = newPath,
                content = content,
                byteCount = byteCount,
                bufferOffset = bufferOffset,
                permissions = permissions
            )
        }
    } else {
        AsyncClosingScope { scope ->
            openUnbufferedFile(scope, handle, path, FileOpenType.WRITE_ONLY, flags, permissions)
                .andAlso {
                    it.writeAll(content, byteCount, bufferOffset)
                }
                .andThen {
                    it.close()
                }
        }
    }
}

/**
 * Writes [byteCount] bytes from [content] to the file at [path], relative to the provided [handle].
 * This will overwrite the file contents.
 *
 * If [atomic] is true, then this file write will be attempted in an atomic matter (i.e. by
 * writing to a temporary file, then renaming it over the existing file). Cancellation will be
 * suppressed during the atomic write. [flags] is ignored.
 *
 * If [atomic] is false, then [flags] will be used to control how the file handle will be opened.
 *
 * The value of [permissions] controls the permissions for the created file if it doesn't exist,
 * or the new file if an atomic overwrite is requested.
 */
@OptIn(LowLevelApi::class, ProvisionalApi::class)
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    content: ByteArray,
    byteCount: UInt = content.size.toUInt(),
    bufferOffset: Int = 0,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<Unit, Fail> {
    return checkIfCancelled()
        .andThen {
            content.checkBuffers(byteCount, bufferOffset).notCancelled()
        }.andThen {
            doWriteAt(handle, path, content, byteCount, bufferOffset, atomic, flags, permissions)
        }
}

/**
 * Writes [byteCount] bytes from [content] to the file at [path]. This will overwrite the file
 * contents.
 */
@OptIn(LowLevelApi::class, ProvisionalApi::class)
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeAt(
    path: F,
    content: ByteArray,
    byteCount: UInt = content.size.toUInt(),
    bufferOffset: Int = 0,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResult<Unit, Fail> =
    writeAt(null, path, content, byteCount, bufferOffset, atomic, flags, permissions)

/**
 * Like [writeAt], but with a [ByteString] parameter instead.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(Unsafe::class)
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    content: ByteString,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<Unit> {
    return writeAt(
        handle,
        path,
        content.unwrap(),
        atomic = atomic,
        flags = flags,
        permissions = permissions
    ) as CancellableResourceResult<Unit>
}

/**
 * Like [writeAt], but with a [ByteString] parameter instead.
 */
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeAt(
    path: F,
    content: ByteString,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<Unit> {
    return writeAt(null, path, content, atomic, flags, permissions)
}

/**
 * Like [writeAt], but with a String parameter instead.
 */
@Suppress("UNCHECKED_CAST")
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeTextAt(
    path: F,
    content: String,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<Unit> {
    return writeAt(
        null,
        path,
        content.encodeToByteArray(),
        atomic = atomic,
        flags = flags,
        permissions = permissions,
    ) as CancellableResourceResult<Unit>
}

/**
 * Like [writeAt], but with a String parameter instead.
 */
@Suppress("UNCHECKED_CAST")
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.writeTextAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    content: String,
    atomic: Boolean = true,
    flags: Set<FileOpenFlags> = setOf(),
    permissions: Set<FilePermissions> = FilePermissions.DEFAULT_FILE
): CancellableResourceResult<Unit> {
    return writeAt(
        handle,
        path,
        content.encodeToByteArray(),
        atomic = atomic,
        flags = flags,
        permissions = permissions,
    ) as CancellableResourceResult<Unit>
}

// only flags that make sense are O_DIRECT (ig?) and O_NOATIME.
/**
 * Reads the contents of the file referred to by [path] (relative to [handle]) into a [ByteString].
 */
@OptIn(ProvisionalApi::class)
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.readAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    flags: Set<FileOpenFlags> = setOf(),
): CancellableResourceResult<ByteString> = AsyncClosingScope { scope ->
    openUnbufferedFile(scope, handle, path, FileOpenType.READ_ONLY, flags)
        .andThen { it.readUntilEof() } as CancellableResourceResult<ByteString>
}

/**
 * Reads the contents of the file referred to by [path] into a [ByteString].
 */
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.readAt(
    path: F,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResourceResult<ByteString> {
    return readAt(null, path, flags)
}

/**
 * Like [readAt], but reads the textual contents of the file instead.
 */
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.readTextAt(
    handle: FilesystemHandle<F, M>?,
    path: F,
    flags: Set<FileOpenFlags> = setOf(),
): CancellableResourceResult<String> =
    readAt(handle, path, flags).andThen { Cancellable.ok(it.decodeToString()) }

/**
 * Like [readAt], but reads the textual contents of the file instead.
 */
public suspend fun <F : PP<F>, M : FM> Filesystem<F, M>.readTextAt(
    path: F,
    flags: Set<FileOpenFlags> = setOf(),
): CancellableResourceResult<String> =
    readAt(path, flags).andThen { Cancellable.ok(it.decodeToString()) }