/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

/**
 * Base error class for any error that is a result of external usage.
 */
public sealed interface ResourceError : Fail

// https://github.com/python/cpython/blob/f4c03484da59049eb62a9bf7777b963e2267d187/PC/errmap.h
// will be helpful for mapping windows errors
// (although not sure how IOCP uses them)
// https://github.com/Fuyukai/Tinlok/blob/642a7d3083e937891d897510e2a5e50e8ed6378f/tinlok-core/src/commonMain/kotlin/tf/lotte/tinlok/exc/OSException.kt
// my previous error hierachy

public const val EPERM: Int = 1
public const val ENOMEM: Int = 12
public const val EINVAL: Int = 22
public const val ENFILE: Int = 23
public const val EMFILE: Int = 24

/** Returned when trying to use a resource that has already been closed. */
public object AlreadyClosedError : ResourceError

/** Helper type alias for functions that return a [ResourceError]. */
public typealias ResourceResult<Success> = Either<Success, ResourceError>
/** Helper type alias for functions that return a cancellable [ResourceError]. */
public typealias CancellableResourceResult<Success> = CancellableResult<Success, ResourceError>

/**
 * Standard error class for system call related errors.
 */
public sealed class SyscallError(
    /** The POSIX errno. */
    public val errno: Int,
) : ResourceError

/**
 * Returned when the error is unknown.
 */
public class UnknownError(errno: Int) : SyscallError(errno)

/** The operation in question was not permitted. Meaning varies based on syscall. */
public object OperationNotPermitted : SyscallError(EPERM)

/** The kernel is out of memory. */
public object OutOfMemory : SyscallError(ENOMEM)

/** An invalid argument was provided to a system function. */
public object InvalidArgument : SyscallError(EINVAL)

/** The entire system has too many files open. */
public object TooManyGlobalFiles : SyscallError(ENFILE)

/** This process has too many files open. */
public object TooManyFiles : SyscallError(EMFILE)


/**
 * Converts an errno into an [SyscallError].
 */
public fun Int.toSysError(): SyscallError =
    when (this) {
        EPERM -> OperationNotPermitted
        ENOMEM -> OutOfMemory
        EINVAL -> InvalidArgument
        ENFILE -> TooManyGlobalFiles
        EMFILE -> TooManyFiles
        else -> UnknownError(this)
    }

/**
 * Converts an errno into an [ResourceResult].
 */
public fun Int.toSysResult(): ResourceResult<Nothing> = Either.err(toSysError())
