package tf.veriny.wishport

import kotlin.experimental.ExperimentalTypeInference

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
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <Success, NewSuccess, Failure : Fail> Either<Success, Failure>.andThen(
    block: (Success) -> Either<NewSuccess, Failure>
): Either<NewSuccess, Failure> =
    when (this) {
        is Ok<Success> -> block(value)
        is Err<Failure> -> this
    }

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <Success, NewSuccess, Failure : Fail> Either<Success, Failure>.andThen(
    block: (Success) -> CancellableResult<NewSuccess, Failure>
): CancellableResult<NewSuccess, Failure> =
    when (this) {
        is Ok<Success> -> block(value)
        is Err<Failure> -> NotCancelled(this)
    }

/**
 * If this is a success, then call the provided function with the unwrapped value and then return
 * this either. If the provided function returns a failure, then instead return that failure.
 */
public inline fun <Success, Failure : Fail> Either<Success, Failure>.andAlso(
    block: (Success) -> Either<Any, Failure>
): Either<Success, Failure> =
    when (this) {
        is Ok<Success> -> {
            val res = block(value)
            // safe cast, <Success> isn't part of us.
            @Suppress("UNCHECKED_CAST")
            if (res.isSuccess) this
            else res as Either<Success, Failure>
        }
        is Err<Failure> -> this
    }

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <Success, Failure : Fail> Either<Success, Failure>.andAlso(
    block: (Success) -> CancellableResult<Any, Failure>
): CancellableResult<Success, Failure> =
    when (this) {
        is Ok<Success> -> {
            val res = block(value)
            // safe cast, <Success> isn't part of us.
            @Suppress("UNCHECKED_CAST")
            if (res.isSuccess) NotCancelled(this)
            else res as CancellableResult<Success, Failure>
        }
        is Err<Failure> -> this.notCancelled()
    }

/**
 * If this is a success, then return the unwrapped value. Otherwise, panicks with the specified
 * message.
 */
public inline fun <Success, F : Fail> Either<Success, F>.expect(
    message: String = "expected a success"
): Success =
    when (this) {
        is Ok<Success> -> value
        is Err<F> -> throw IllegalStateException(message)
    }

/**
 * If this is a success, calls [left]. Otherwise, calls [right].
 */
public inline fun <Out, Success, F : Fail> Either<Success, F>.fold(
    left: (Success) -> Out,
    right: (F) -> Out,
): Out =
    when (this) {
        is Ok<Success> -> left(value)
        is Err<F> -> right(value)
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
