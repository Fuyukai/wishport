/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

/**
 * Special object used to wrap a throwable that is caught inside a function.
 */
public value class ThrowableFailure(public val exception: Throwable) : Fail

/**
 * Converts a Kotlin [Result] into a Wishport [Either].
 */
public fun <Success> Result<Success>.intoEither(): Either<Success, ThrowableFailure> {
    return if (isSuccess) {
        Either.ok(getOrThrow())
    } else {
        // null safe!
        Either.err(ThrowableFailure(this.exceptionOrNull()!!))
    }
}

// boundaries kinda break down here.
/**
 * Converts a Kotlin [Result] that encapsulates an [Either] into a regular [Either].
 *
 * This destroys the Failure type signature and replacees it with [Fail], unfortunately.
 */
public fun <Success, Failure : Fail> Result<Either<Success, Failure>>
.intoEither(): Either<Success, Fail> {
    return if (isSuccess) {
        getOrThrow()
    } else {
        // safe null check again
        Either.err(ThrowableFailure(this.exceptionOrNull()!!))
    }
}

/**
 * Converts a Kotlin [Result] that encapsulates an [CancellableResult] into a regular
 * [CancellableResult].
 */
public fun <S, F : Fail> Result<CancellableResult<S, F>>.intoCancellableResult(): CancellableResult<S, Fail> {
    return if (isSuccess) {
        getOrThrow()
    } else {
        // safe null check again
        Either.err(ThrowableFailure(this.exceptionOrNull()!!)).notCancelled()
    }
}
