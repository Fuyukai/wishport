/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.*
import tf.veriny.wishport.internals.*
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.io.net.*

// workaround for weird optin stuff
/**
 * Variant of [runUntilComplete] that doesn't require a [Clock].
 */
@OptIn(LowLevelApi::class)
public fun <S, F : Fail> runUntilComplete(
    fn: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    return runUntilComplete(null, fn)
}

// XXX: Kotlin forces this overload no matter what so it's hackily renamed.
// Not sure why this is the case.

// workaround for weird optin stuff
/**
 * Variant of [runUntilCompleteNoResult] that doesn't require a [Clock].
 */
@OptIn(LowLevelApi::class)
public fun runUntilCompleteNoResult(fn: suspend () -> Unit) {
    runUntilCompleteNoResult(null, fn)
}

/**
 * Gets the current time according to the event loop, in nanoseconds. This is a "fake" suspendable,
 * i.e. it doesn't abide by cancellation or returning an Either.
 */
@OptIn(LowLevelApi::class)
public suspend fun getCurrentTime(): Long {
    return EventLoop.get().clock.getCurrentTime()
}

/**
 * Causes a checkpoint. This will allow yielding to the event loop for other tasks to do their work
 * before returning here.
 *
 * This variant allows passing a value for easier monad chaining, e.g.:
 *
 * ```kotlin
 *
 * return someOperation().andThen { checkpoint(it) }
 *
 * ```
 */
@OptIn(LowLevelApi::class)
public suspend fun <S, F : Fail> checkpoint(value: S): CancellableResult<S, F> {
    val task = getCurrentTask()
    return task.checkpoint(value)
}

/**
 * Causes a checkpoint. This will allow yielding to the event loop for other tasks to do their work
 * before returning here.
 *
 * This variant does not have a value.
 */
public suspend fun checkpoint(): CancellableEmpty {
    return checkpoint(Unit)
}

/**
 * Causes a checkpoint that cannot be cancelled. This will allow yielding to the event loop for
 * other tasks to do their work before returning here.
 *
 * This is used to enforce Wishport's cancellation semantics; either a cancellation happened, or
 * the operation completed. A regular checkpoint can cause a cancellation even if the operation
 * happened, leading to inconsistent state.
 */
@OptIn(LowLevelApi::class)
public suspend fun <S> uncancellableCheckpoint(value: S): CancellableSuccess<S> {
    val task = getCurrentTask()
    return task.uncancellableCheckpoint(value)
}

/**
 * Like [uncancellableCheckpoint], but this doesn't take a value.
 */
@OptIn(LowLevelApi::class)
public suspend fun uncancellableCheckpoint(): CancellableEmpty {
    return uncancellableCheckpoint(Unit)
}

/**
 * Waits until all other tasks are currently blocked (waiting to be rescheduled). This will
 * ONLY fire if there are no possible other tasks that can run on the next iteration of
 * the event loop.
 *
 * Rescheduling this task manually is NOT supported!
 */
@OptIn(LowLevelApi::class)
public suspend fun waitUntilAllTasksAreBlocked(): CancellableEmpty {
    val task = getCurrentTask()
    task.isWaitUntilAll = true
    val loop = task.context.eventLoop

    // safe cast, we only get rescheduled
    return task.checkIfCancelled()
        .andThen {
            loop.waitingAllTasksBlocked = task
            task.suspendTask().also { task.isWaitUntilAll = false }
        } as CancellableEmpty
}

// == Sleep/Timeout Helpers == //
// structured similarly to trio
/**
 * Runs the specified [block] with a timeout of [nanos] nanoseconds. If the actions within do not
 * complete in time, then the function is cancelled.
 */
public suspend inline fun <S, F : Fail> moveOnAfter(
    nanos: Long,
    crossinline block: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    return CancelScope { cs ->
        cs.localDeadline = getCurrentTime() + nanos
        block()
    }
}

/**
 * Runs the specified [block], timing out at the exact time specified by [nanos]. If the actions
 * within do not complete in time, then the function is cancelled.
 *
 * If the time specified is in the past, then this will perform a checkpoint, and all results inside
 * the block will be cancelled.
 */
public suspend inline fun <S, F : Fail> moveOnAt(
    nanos: Long,
    crossinline block: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    // note: don't need to return a cancelled here (imo) as it's less surprising the code within the
    // block gets executed, but always returns cancelled.
    // we do want to perform a checkpoint to allow yielding.
    if (nanos < getCurrentTime()) {
        checkpoint()
    }

    return CancelScope { cs ->
        cs.localDeadline = nanos
        block()
    }
}

/**
 * Sleeps forever (or, until cancelled).
 */
@OptIn(LowLevelApi::class)
public suspend fun sleepForever(): CancellableResult<Any?, Fail> {
    return checkIfCancelled().andThen { waitUntilRescheduled() }
}

/**
 * Sleeps until the specified time, in nanoseconds.
 */
@OptIn(LowLevelApi::class)
public suspend fun sleepUntil(time: Long): CancellableEmpty {
    moveOnAt(time) { sleepForever() }
    return checkIfCancelled()
}

/**
 * Sleeps for the specified amount of nanoseconds, returning an empty value.
 */
@OptIn(LowLevelApi::class)
public suspend fun sleep(nanos: Long): CancellableEmpty {
    return sleepUntil(getCurrentTime() + nanos)
}

/**
 * Runs the specified [block] asynchronously in a separate worker thread.
 *
 * This will call [producer] to capture any local variables that may be required, then pass the
 * return result to [block].
 */
@OptIn(LowLevelApi::class)
public suspend fun <P, S, F : Fail> runSynchronouslyOffThread(
    producer: () -> P,
    cancellable: Boolean = false,
    block: (P) -> Either<S, F>
): CancellableResult<S, F> {
    val loop = EventLoop.get()
    return loop.workerPool.runSyncInThread(cancellable, producer, block)
}

/**
 * Looks up an IP address from its hostname and port.
 */
@OptIn(LowLevelApi::class)
public suspend fun <T : EndpointInfo> getAddressFromName(
    hostname: String,
    port: Int,
    socketType: SocketType,
    socketFamily: SocketFamily? = null,
    socketProtocol: SocketProtocol? = null,
): CancellableResult<T, Fail> {
    val loop = EventLoop.get()
    return loop.nameResolver.getAddressFromName(
        hostname, port, socketType, socketFamily, socketProtocol
    )
}

// == I/O helpers == //
/**
 * Opens a new [UnbufferedFile] for the specified [path], relative to the [otherHandle],
 * with the specified [mode] and file open [flags]. This function needs to be ran inside an
 * [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun AsyncClosingScope.openUnbufferedSystemFile(
    otherHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>,
    path: SystemPurePath,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<UnbufferedFile, Fail> =
    SystemFilesystem.getRelativeFileHandle(otherHandle, path, mode, flags)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
        .andAddTo(this)

/**
 * Like [openUnbufferedSystemFile], but takes a string argument.
 */
@ProvisionalApi
public suspend fun AsyncClosingScope.openUnbufferedSystemFile(
    otherHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>,
    path: String,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<UnbufferedFile, Fail> =
    openUnbufferedSystemFile(otherHandle, systemPathFor(path), mode, flags)

/**
 * Opens a new [UnbufferedFile] for the specified [path], with the specified [mode] and file open
 * [flags]. This function needs to be ran inside an [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun AsyncClosingScope.openUnbufferedSystemFile(
    path: SystemPurePath,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<UnbufferedFile, Fail> =
    SystemFilesystem.getFileHandle(path, mode, flags)
        .andThen { Cancellable.ok(UnbufferedFile(it)) }
        .andAddTo(this)

/**
 * Like [openBufferedSystemFile], but takes a string argument.
 */
@ProvisionalApi
public suspend fun AsyncClosingScope.openUnbufferedSystemFile(
    path: String,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<UnbufferedFile, Fail> =
    openUnbufferedSystemFile(systemPathFor(path), mode, flags)

/**
 * Opens a new [BufferedFile] for the specified [path], relative to the [otherHandle],
 * with the specified [mode] and file open [flags]. This function needs to be ran inside an
 * [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun AsyncClosingScope.openBufferedSystemFile(
    otherHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>,
    path: SystemPurePath,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<BufferedFile, Fail> =
    SystemFilesystem.getRelativeFileHandle(otherHandle, path, mode, flags)
        .andThen { BufferedFile(it) }
        .andAddTo(this)

/**
 * Like [openBufferedSystemFile], but takes a string argument.
 */
@ProvisionalApi
public suspend fun AsyncClosingScope.openBufferedSystemFile(
    otherHandle: FilesystemHandle<SystemPurePath, PlatformFileMetadata>,
    path: String,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<BufferedFile, Fail> =
    openBufferedSystemFile(otherHandle, systemPathFor(path), mode, flags)

/**
 * Opens a new [BufferedFile] for the specified [path], with the specified [mode] and file open
 * [flags]. This function needs to be ran inside an [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun AsyncClosingScope.openBufferedSystemFile(
    path: SystemPurePath,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<BufferedFile, Fail> =
    SystemFilesystem.getFileHandle(path, mode, flags)
        .andThen { BufferedFile(it) }
        .andAddTo(this)

/**
 * Like [openBufferedSystemFile], but takes a string argument.
 */
@ProvisionalApi
public suspend fun AsyncClosingScope.openBufferedSystemFile(
    path: String,
    mode: FileOpenType = FileOpenType.READ_ONLY,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<BufferedFile, Fail> =
    openBufferedSystemFile(systemPathFor(path), mode, flags)

/**
 * Opens a new TCP stream to the specified [SocketAddress].
 */
@OptIn(Unsafe::class, LowLevelApi::class)
@ProvisionalApi
public suspend fun <T : SocketAddress> AsyncClosingScope.openTcpStream(
    address: T
): CancellableResult<TcpSocketStream, Fail> {
    if (address.protocol != SocketProtocol.TCP) return Cancellable.failed(ProtocolNotSupported)

    return Socket(address)
        .andAddTo(this)
        .andAlso { it.connect(address) }
        .andThen { Cancellable.ok(TcpSocketStream(it)) }
}
