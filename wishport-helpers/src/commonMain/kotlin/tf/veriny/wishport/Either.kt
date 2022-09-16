/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

/**
 * Marker interface for all failure types. Named Fail to avoid conflicting with kotlin.Error.
 */
public interface Fail

/**
 * Simple error returning type. Used for all Wishport functionality, as exceptions don't work
 * that well in the asynchronous world.
 */
public sealed interface Either<out Success, out Failure : Fail> {
    public companion object {
        public inline fun <Success> ok(v: Success): Either<Success, Nothing> =
            Ok(v)

        public inline fun <Failure : Fail> err(v: Failure): Either<Nothing, Failure> =
            Err(v)
    }
}

/**
 * A successful result.
 */
@PublishedApi
internal data class Ok<out Success>(public val value: Success) : Either<Success, Nothing>

/**
 * A non-successful result.
 */
@PublishedApi
internal data class Err<out Failure : Fail>(public val value: Failure) : Either<Nothing, Failure>

// helper extensions
public inline val Either<*, *>.isSuccess: Boolean get() = this is Ok<*>
public inline val Either<*, *>.isFailure: Boolean get() = this is Err<*>

/**
 * Converts this [Either] into a nullable [Success].
 */
public inline fun <Success, Failure : Fail> Either<Success, Failure>.get(): Success? =
    when (this) {
        is Ok<Success> -> value
        is Err<Failure> -> null
    }

/**
 * Converts this [Either] into a nullable [Failure].
 */
public inline fun <Success, Failure : Fail> Either<Success, Failure>.getFailure(): Failure? =
    when (this) {
        is Ok<Success> -> null
        is Err<Failure> -> value
    }

/**
 * If this is a success, then call the provided function with the unwrapped value. Otherwise,
 * return the failure. This is sometimes confusingly known as ``flatMap``.
 */
public inline fun <Success, NewSuccess, Failure : Fail> Either<Success, Failure>.andThen(
    block: (Success) -> Either<NewSuccess, Failure>
): Either<NewSuccess, Failure> =
    when (this) {
        is Ok<Success> -> block(value)
        is Err<Failure> -> this
    }

/**
 * If this is a success, then return the unwrapped value. Otherwise, return the constant provided
 * to this function.
 */
public inline fun <Out, Success : Out, Failure : Fail> Either<Success, Failure>.unwrapOr(
    value: Out
): Out =
    when (this) {
        is Ok<Success> -> this.value
        is Err<*> -> value
    }

/**
 * If this is a success, then return the success. Otherwise, call the provided function with the
 * unwrapped failure and return the result of that function.
 */
public inline fun <Out, Success : Out, Failure : Fail> Either<Success, Failure>.unwrapOrElse(
    block: (Failure) -> Out
): Out =
    when (this) {
        is Ok<Success> -> value
        is Err<Failure> -> block(value)
    }

/**
 * Runs the specified block safely, converting potential thrown errors
 */
public inline fun <Success> runSafely(block: () -> Success): Either<Success, ThrowableFailure> {
    return try {
        Either.ok(block())
    } catch (e: Throwable) {
        Either.err(ThrowableFailure(e))
    }
}
