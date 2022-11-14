/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

/**
 * A monad for representing cancellable results.
 *
 * Any asynchronous Wishport function can be cancelled (with a few very special exceptions).
 */
public sealed interface Cancellable<out S, out F : Fail, out T : Either<S, F>> {
    public companion object {
        private val EMPTY = ok(Unit)

        /**
         * Helper function for ``Either.ok(item).notCancelled()``.
         */
        public fun <Success> ok(item: Success): CancellableResult<Success, Nothing> {
            return NotCancelled(Ok(item))
        }

        /**
         * Helper function for ``Either.err(item).notCancelled()``.
         */
        public fun <Failure : Fail> failed(item: Failure): CancellableResult<Nothing, Failure> {
            return NotCancelled(Err(item))
        }

        /**
         * Helper function for returning a cancel. Not that useful.
         */
        public fun cancelled(): CancellableResult<Nothing, Nothing> {
            return Cancelled
        }

        /**
         * Returns an empty uncancelled.
         */
        public fun empty(): CancellableEmpty {
            return EMPTY
        }
    }
}

public typealias CancellableResult<Success, Failure> =
    Cancellable<Success, Failure, Either<Success, Failure>>

public typealias CancellableSuccess<Success> = CancellableResult<Success, Nothing>

public typealias CancellableEmpty = CancellableSuccess<Unit>

/**
 * Wrapper for when a function is *not* cancelled.
 */
@PublishedApi
internal data class NotCancelled<out S, out F : Fail, out T : Either<S, F>>(
    val wrapped: T,
) : Cancellable<S, F, T>

/**
 * Returned when a function *is* cancelled.
 */
@PublishedApi
internal object Cancelled : Cancellable<Nothing, Nothing, Nothing> {
    override fun toString(): String {
        return "Cancelled"
    }
}

public inline val CancellableResult<*, *>.isSuccess: Boolean get() =
    this is NotCancelled && wrapped is Ok

public inline val CancellableResult<*, *>.isFailure: Boolean get() =
    this is NotCancelled && wrapped is Err

public inline val CancellableResult<*, *>.isCancelled: Boolean get() =
    this is Cancelled

/**
 * Unwraps the [Either] contained within this [CancellableResult]
 */
public inline fun <S, F : Fail> CancellableResult<S, F>.getEither(): Either<S, F>? =
    when (this) {
        is NotCancelled<S, F, Either<S, F>> -> wrapped
        is Cancelled -> null
    }

/**
 * Converts this [CancellableResult] into a nullable [S].
 */
public inline fun <S, F : Fail> CancellableResult<S, F>.get(): S? {
    return if (this is Cancelled) null
    else {
        val value = (this as NotCancelled<S, F, Either<S, F>>).wrapped
        value.get()
    }
}

/**
 * Converts this [Either] into a nullable [Failure].
 */
public inline fun <S, F : Fail> CancellableResult<S, F>.getFailure(): F? {
    return if (this is Cancelled) null
    else {
        val value = (this as NotCancelled<S, F, Either<S, F>>).wrapped
        value.getFailure()
    }
}

/**
 * Converts a plain [Either] into a non-cancelled result.
 */
public inline fun <S, F : Fail> Either<S, F>.notCancelled(): CancellableResult<S, F> {
    return NotCancelled(this)
}

/**
 * If this is a non-cancelled success, then call the provided function with the unwrapped value.
 * Otherwise, return the failure. This is sometimes confusingly known as ``flatMap``.
 */
public inline fun <NewSuccess, S, F : Fail> CancellableResult<S, F>.andThen(
    block: (S) -> CancellableResult<NewSuccess, F>
): CancellableResult<NewSuccess, F> =
    when (this) {
        is Cancelled -> this
        is NotCancelled<S, F, Either<S, F>> -> {
            @Suppress("UNCHECKED_CAST")
            when (wrapped) {
                is Ok<S> -> block(wrapped.value)
                // safe cast as the NewSuccess is simply not part of us.
                is Err<F> -> this as CancellableResult<NewSuccess, F>
            }
        }
    }

/**
 * If this is a success, then call the provided function with the unwrapped value and then return
 * this either. If the provided function returns a failure, then instead return that failure.
 */
public inline fun <Success, Failure : Fail> CancellableResult<Success, Failure>.andAlso(
    block: (Success) -> CancellableResult<Any, Failure>
): CancellableResult<Success, Failure> =
    when (this) {
        is Cancelled -> this
        is NotCancelled<Success, Failure, Either<Success, Failure>> -> {
            when (wrapped) {
                is Ok<Success> -> {
                    val res = block(wrapped.value)
                    // safe cast, <Success> isn't part of us.
                    if (res.isSuccess) this
                    else res as CancellableResult<Success, Failure>
                }
                is Err<Failure> -> this
            }
        }
    }

/**
 * If this is a non-cancelled success, then return the unwrapped value. Otherwise, return the
 * constant provided to this function.
 */
public inline fun <Out, S : Out, F : Fail> CancellableResult<S, F>.unwrapOr(
    value: Out
): Out =
    when (this) {
        is Cancelled -> value
        is NotCancelled<S, F, Either<S, F>> -> {
            when (wrapped) {
                is Ok<S> -> wrapped.value
                is Err<F> -> value
            }
        }
    }

// idk what a good name for this would be. its clearly not a fold.
/**
 * If this is a non-cancelled success, calls [left] with the success. If this is a non-cancelled
 * failure, calls [right] with the failure. Otherwise, returns Cancelled.
 */
public inline fun <Out, S, F : Fail> CancellableResult<S, F>.combinate(
    left: (S) -> CancellableResult<Out, Fail>,
    right: (F) -> CancellableResult<Out, Fail>
): CancellableResult<Out, Fail> =
    when (this) {
        is Cancelled -> this
        is NotCancelled<S, F, Either<S, F>> -> {
            when (wrapped) {
                is Ok<S> -> left(wrapped.value)
                is Err<F> -> right(wrapped.value)
            }
        }
    }

/**
 * If this is a non-cancelled success, then return the unwrapped value. Otherwise, panic with the
 * specified error message.
 */
public inline fun <S, F : Fail> CancellableResult<S, F>.expect(
    message: String = "expected a success"
): S =
    when (this) {
        is Cancelled -> throw IllegalStateException(message)
        is NotCancelled<S, F, Either<S, F>> -> {
            wrapped.expect(message)
        }
    }
