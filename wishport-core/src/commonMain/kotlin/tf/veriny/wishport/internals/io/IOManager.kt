/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.StableApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.*
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.io.net.SocketAddress

// TODO: Rethink if this should be responsible for I/O dispatching itself, or if that functionality
//       should be moved to the event loop, which can then poll this.

/**
 * Responsible for handling I/O within the event loop. This entirely abstracts away how I/O works
 * per-platform, and is responsible internally for its own set of suspended tasks.
 */
@LowLevelApi
@StableApi
public expect class IOManager : Closeable {
    public companion object {
        public fun default(): IOManager

        public fun withSize(size: Int): IOManager
    }

    /**
     * Peeks off all pending I/O events, and wakes up tasks that would be waiting. Returns the
     * number of tasks woken up.
     */
    public fun pollIO(): Int

    /**
     * Waits for I/O forever. Returns the number of tasks woken up.
     */
    public fun waitForIO(): Int

    /**
     * Waits for I/O for [timeout] nanoseconds. Returns the number of tasks woken up.
     */
    public fun waitForIOUntil(timeout: Long): Int

    /**
     * Forces a wake up of the I/O manager, either now (if it is blocked on I/O) or at the next
     * iteration of the event loop. This should only be called off-thread.
     */
    public fun forceWakeUp()

    // == actual I/O methods == //

    /**
     * Closes an [IOHandle] asynchronously. This method is uncancellable; you must wait for it to
     * return.
     */
    public suspend fun closeHandle(handle: IOHandle): CancellableResourceResult<Empty>

    /**
     * Attempts to close multiple handles in a more efficient manner than repeatedly calling
     * closeHandle(). This will return the FIRST error encountered.
     */
    public suspend fun closeMany(vararg handles: IOHandle): CancellableResourceResult<Empty>

    /**
     * Shuts down one or both sides of an [IOHandle], without closing it.
     */
    public suspend fun shutdown(
        handle: IOHandle,
        how: ShutdownHow
    ): CancellableResourceResult<Empty>

    /**
     * Closes a [SocketHandle] in a more efficient way if supported. This obeys the same rules as
     * [closeHandle].
     */
    public suspend fun closeSocket(socket: SocketHandle): CancellableResourceResult<Empty>

    // CreateFileEx isn't async so we have to punt it off to a worker on windows.

    /**
     * Opens a directory on the real filesystem, returning a directory handle.
     *
     * If [dirHandle] is provided, the directory will be opened relative to it. If null is provided,
     * then the directory will be opened relative to the current working directory. If path is an
     * absolute path, then the provided handle is ignored.
     */
    @Unsafe
    public suspend fun openFilesystemDirectory(
        dirHandle: DirectoryHandle?,
        path: ByteString,
    ): CancellableResourceResult<DirectoryHandle>

    /**
     * Opens a file on the real filesystem, returning a file handle.
     *
     * If [dirHandle] is provided, the file will be opened relative to it. If null is provided,
     * then the file will be opened relative to the current working directory. If path is an
     * absolute path, then the provided handle is ignored.
     *
     * [flags] is a set of [FileOpenFlags] that control the behaviour of the open file handle.
     * [filePermissions] is a set of [FilePermissions] that define the permissions set on a file
     * if it is created with [FileOpenFlags.CREATE_IF_NOT_EXISTS]. Otherwise, [filePermissions]
     * is ignored.
     */
    @Unsafe
    public suspend fun openFilesystemFile(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        type: FileOpenType,
        flags: Set<FileOpenFlags>,
        filePermissions: Set<FilePermissions>
    ): CancellableResourceResult<RawFileHandle>

    /**
     * Binds the socket [sock] to the specified [address].
     */
    public suspend fun bind(sock: SocketHandle, address: SocketAddress): CancellableResourceResult<Empty>

    /**
     * Connects the socket [sock] to the specified [address].
     */
    public suspend fun connect(
        sock: SocketHandle,
        address: SocketAddress
    ): CancellableResourceResult<Empty>

    /**
     * Accepts a new connection for the specified [sock].
     */
    @Unsafe
    public suspend fun accept(sock: SocketHandle): CancellableResourceResult<SocketHandle>

    /**
     * Reads up to [size] bytes from a [IOHandle] into [out], starting at [fileOffset] from the
     * file's current position, and at [bufferOffset] into the provided buffer.
     */
    public suspend fun read(
        handle: IOHandle,
        out: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int,
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Writes up to [size] bytes from [input] into an [IOHandle], starting from [bufferOffset] in the
     * provided buffer, and into [fileOffset] from the file's current position.
     */
    public suspend fun write(
        handle: IOHandle,
        input: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail>

    // TODO: make flags higher-level?

    /**
     * Reads up to [size] bytes from a connected socket-based [handle] into [out], starting at
     * offset [bufferOffset], using the socket-specific [flags].
     */
    public suspend fun recv(
        handle: IOHandle,
        out: ByteArray,
        size: UInt,
        bufferOffset: Int,
        flags: Int
    ): CancellableResult<ByteCountResult, Fail>

    /**
     * Writes up to [size] bytes from the specified buffer [input] into a connected socket-based
     * [handle], starting at offset [bufferOffset], using the socket-specific [flags].
     */
    public suspend fun send(
        handle: IOHandle,
        input: ByteArray,
        size: UInt,
        bufferOffset: Int,
        flags: Int
    ): CancellableResult<ByteCountResult, Fail>

    // FlushFileEx is not async
    /**
     * Forces a file flush for the specified [handle]. If [withMetadata] is specified, then all
     * metadata will be flushed too. Otherwise, only metadata required for read consistency is
     * flushed. (This can be thought of as like fsync() vs fdatasync()).
     */
    public suspend fun fsync(
        handle: IOHandle,
        withMetadata: Boolean
    ): CancellableResourceResult<Empty>

    /**
     * Seeks a seekable file [handle] to the specified [position], dependant on the behaviour
     * controlled by passing [whence], reeturning the new file offset.
     */
    public suspend fun lseek(
        handle: IOHandle,
        position: Long,
        whence: SeekWhence,
    ): CancellableResourceResult<SeekPosition>

    /**
     * Gets the metadata for a file on the filesystem.
     *
     * If [handle] is provided, and [path] is not null, then the metadata will be for a file
     * relative to [handle]. In this case, [handle] must be a [DirectoryHandle].
     * If [handle] is provided, and [path] is null, then the metadata for be for the file identified
     * by [handle].
     *
     * If [handle] is not provided, then [path] must not be null, and the metadata will be for the
     * file at [path].
     */
    public suspend fun fileMetadataAt(
        handle: IOHandle?,
        path: ByteString?,
    ): CancellableResourceResult<PlatformFileMetadata>

    /**
     * Gets the full real path for the specified open file.
     */
    public suspend fun realPathOf(handle: IOHandle): CancellableResourceResult<ByteString>

    /**
     * Gets a list of [DirectoryEntry] instances for the specified open directory.
     */
    public suspend fun getDirectoryEntries(
        handle: IOHandle
    ): CancellableResourceResult<List<DirectoryEntry>>

    // io_uring has io_uring_prep_poll_add/poll_remove
    // but windows has these absolute bastard methods in winsock that suck fuck to use
    // WSAPoll 1) only supports 512 sockets (not true) 2) is O(n) (lol)
    // WaitForEvent shit is just terrible overall
    // IOCP doesn't work properly by default!
    // so instead we just do what all the cool kids (read: trio) does and talk directly to AFD, the
    // kernel socket driver. if M$ doesn't want us doing this stuff, they should stop encouraging
    // usage of the Nt functions in their docs instead.

    /**
     * Polls a handle for readiness. Returns the set of events the handle is ready for.
     */
    public suspend fun pollHandle(
        handle: IOHandle,
        what: Set<Poll>
    ): CancellableResourceResult<PollResult>

    /**
     * Creates a new directory.
     *
     * If [dirHandle] is provided, the new directory will be relative to it. If null is provided,
     * then the new directory will be relative to the current working directory. If the path is an
     * absolute path, then the provided handle is ignored.
     *
     * [permissions] is a set of permissions to create the new directory with. If this is unset,
     * then the default permissions (0755) will be used.
     */
    public suspend fun makeDirectoryAt(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        permissions: Set<FilePermissions>
    ): CancellableResourceResult<Empty>

    /**
     * Renames a file or directory.
     *
     * If [fromDirHandle] is provided, the existing path [from] will be relative to it. If null is
     * provided, then the existing path will be relative to the current working directory. If the
     * path is an absolute path, then the provided handle is ignored.
     *
     * If [toDirHandle] is provided, the existing path [to] will be relative to it. If null is
     * provided, then the existing path will be relative to the current working directory. If the
     * path is an absolute path, then the provided handle is ignored.
     *
     * [flags] is a set of zero or more flags used to customise the behaviour of the rename
     * operation.
     */
    public suspend fun renameAt(
        fromDirHandle: DirectoryHandle?,
        from: ByteString,
        toDirHandle: DirectoryHandle?,
        to: ByteString,
        flags: Set<RenameFlags>
    ): CancellableResourceResult<Empty>

    /**
     * Creates a hardlink to the file at [from], with the link existing at [to].
     *
     * If [fromDirHandle] is provided, the existing path [from] will be relative to it. If null is
     * provided, then the existing path will be relative to the current working directory. If the
     * path is an absolute path, then the provided handle is ignored.
     *
     * If [toDirHandle] is provided, the existing path [to] will be relative to it. If null is
     * provided, then the existing path will be relative to the current working directory. If the
     * path is an absolute path, then the provided handle is ignored.
     */
    public suspend fun linkAt(
        fromDirHandle: DirectoryHandle?,
        from: ByteString,
        toDirHandle: DirectoryHandle?,
        to: ByteString,
    ): CancellableResourceResult<Empty>

    /**
     * Creates a symbolic link with the content [target], at [newPath].
     *
     * If [dirHandle] is provided, the existing path [to] will be relative to it. If null is
     * provided, then the existing path will be relative to the current working directory. If the
     * path is an absolute path, then the provided handle is ignored.
     */
    public suspend fun symlinkAt(
        target: ByteString,
        dirHandle: DirectoryHandle?,
        newPath: ByteString,
    ): CancellableResourceResult<Empty>

    /**
     * Removes or unlinks a file relative to the specified directory.
     *
     * If [dirHandle] is provided, the unlinked file will be relative to it. If null is provided,
     * then the unlinked file will be relative to the current working directory. If path is an
     * absolute path, then the provided handle is ignored.
     *
     * If [removeDir] is true, then if this path refers to an empty directory, it will be deleted.
     * Otherwise, it will error with EISDIR.
     */
    public suspend fun unlinkAt(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        removeDir: Boolean,
    ): CancellableResourceResult<Empty>
}
