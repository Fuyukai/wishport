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

/** Returned when trying to use a resource that has already been closed. */
public object AlreadyClosedError : ResourceError

/** Returned when reaching an EOF in a buffered file. */
public object EndOfFileError : ResourceError

/** Returned if there is not enough data available in the buffer to complete the read request. */
public object NotEnoughDataError : ResourceError

/** Helper type alias for functions that return a [ResourceError]. */
public typealias ResourceResult<Success> = Either<Success, ResourceError>
/** Helper type alias for functions that return a cancellable [ResourceError]. */
public typealias CancellableResourceResult<Success> = CancellableResult<Success, ResourceError>
/** Helper type alias for functions that return a cancellable Unit + [ResourceError]. */
public typealias CancellableResourceEmpty = CancellableResult<Unit, ResourceError>

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
public class UnknownError(errno: Int) : SyscallError(errno) {
    override fun toString(): String {
        return "UnknownError[errno = $errno]"
    }
}

/**
 * Converts an errno into an [SyscallError].
 */
public fun Int.toSysError(): SyscallError =
    ERRNO_MAPPING.getOrNull(this) ?: UnknownError(this)

/**
 * Converts an errno into an [ResourceResult].
 */
public fun Int.toSysResult(): ResourceResult<Nothing> = Either.err(toSysError())
