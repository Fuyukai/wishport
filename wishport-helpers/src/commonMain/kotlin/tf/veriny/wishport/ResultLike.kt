/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.collections.NonEmptyList
import tf.veriny.wishport.collections.nonEmptyListOf

/**
 * Marker interface for all failure types. Named Fail to avoid conflicting with kotlin.Error.
 */
public interface Fail

/**
 * The base interface for both [Either] and [Validated].
 */
public sealed interface ResultLike<out Success, out Failure : Fail>

/**
 * Simple error returning type. Used for all Wishport functionality, as exceptions don't work
 * that well in the asynchronous world.
 */
public sealed interface Either<out Success, out Failure : Fail> : ResultLike<Success, Failure> {
    public companion object {
        @PublishedApi
        internal val UNIT: Either<Unit, Nothing> = Ok(Unit)

        @Suppress("UNCHECKED_CAST")  // actually safe
        public inline fun <Success> ok(v: Success): Either<Success, Nothing> =
            if (v == Unit) UNIT as Either<Success, Nothing> else Ok(v)

        public inline fun <Failure : Fail> err(v: Failure): Either<Nothing, Failure> =
            Err(v)
    }
}

/**
 * Like an [Either], but can be used to gather up errors rather than short circuiting.
 */
public sealed interface Validated<out Success, out Failure : Fail> : ResultLike<Success, Failure> {
    public companion object {
        public inline fun <Success> ok(v: Success): Validated<Success, Nothing> =
            Ok(v)

        public inline fun <Failure : Fail> err(v: Failure): Validated<Nothing, Failure> =
            MultiErr(nonEmptyListOf(v))

        public inline fun <Failure : Fail> err(
            v: Failure,
            vararg vs: Failure
        ): Validated<Nothing, Failure> =
            MultiErr(nonEmptyListOf(v, *vs))

        public inline fun <Failure : Fail> err(
            vs: NonEmptyList<Failure>
        ): Validated<Nothing, Failure> =
            MultiErr(vs)
    }
}

/**
 * A successful result.
 */
@PublishedApi
internal data class Ok<out Success>(val value: Success) : Either<Success, Nothing>, Validated<Success, Nothing>

/**
 * A non-successful result.
 */
@PublishedApi
internal data class Err<out Failure : Fail>(val value: Failure) : Either<Nothing, Failure>

/**
 * Like [Err], but for [Validated].
 */
@PublishedApi
internal data class MultiErr<Failure : Fail>(
    val failures: NonEmptyList<Failure>,
) : Validated<Nothing, Failure>

// == common extensions == //
// helper extensions
public inline val ResultLike<*, *>.isSuccess: Boolean get() = this is Ok<*>
public inline val ResultLike<*, *>.isFailure: Boolean get() = this !is Ok<*>

/**
 * Converts this [ResultLike] into a nullable [Success].
 */
public inline fun <Success, Failure : Fail> ResultLike<Success, Failure>.get(): Success? =
    when (this) {
        is Ok<Success> -> value
        else -> null
    }

/**
 * Runs the specified block safely, converting potential thrown errors and turning them into
 * [ThrowableFailure]. This is most useful for interfacing with the Kotlin standard library.
 */
public inline fun <Success> runSafely(block: () -> Success): Either<Success, ThrowableFailure> {
    return try {
        Either.ok(block())
    } catch (e: Throwable) {
        Either.err(ThrowableFailure(e))
    }
}
