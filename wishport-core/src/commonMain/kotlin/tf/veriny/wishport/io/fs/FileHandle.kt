package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.internals.io.ByteCountResult
import tf.veriny.wishport.internals.io.ReadableHandle
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode


/**
 * A handle to an opened file on a filesystem. This provides methods to perform I/O based on the
 */
public interface FileHandle<F : PurePath<F>> : Closeable {
    public companion object;

    /** The filesystem this handle is open on. */
    public val filesystem: Filesystem<F>

    /** The raw system file handle for this FileHandle, for usage in backend I/O. */
    @LowLevelApi
    public val raw: ReadableHandle

    /** The path to this file. */
    public val path: F

    /**
     * Opens a file relative to this file if (and only if) this file is a directory. This will
     * fail with ENOTDIR otherwise.
     */
    @Unsafe
    public suspend fun openRelative(
        path: F,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResult<FileHandle<F>, Fail>

    /**
     * Reads data from the file handle into the specified [buf]. The data will be read from the file
     * at offset [fileOffset], for [size] bytes, and copied into the buffer at [bufferOffset].
     *
     * If any of these are out of bounds, then this will return [IndexOutOfRange] or [TooSmall].
     */
    public suspend fun readInto(
        buf: ByteArray, size: UInt, bufferOffset: Int, fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail>
}