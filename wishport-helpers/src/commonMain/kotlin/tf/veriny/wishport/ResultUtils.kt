/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlin.experimental.ExperimentalTypeInference

/**
 * Repeatedly runs the specified [fn]. until it results in a failure.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <S, F : Fail> repeatedly(fn: () -> Either<S, F>): Either<Nothing, F> {
    while (true) {
        val result = fn()
        if (result.isFailure) return result as Either<Nothing, F>
    }
}

/**
 * Repeatedly runs the specified [fn]. until it results in a failure.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <S, F : Fail> repeatedly(
    fn: () -> CancellableResult<S, F>
): CancellableResult<Nothing, F> {
    while (true) {
        val result = fn()
        if (result.isFailure) return result as CancellableResult<Nothing, F>
    }
}

// TODO: we want some sort of Validated helper i guess. like arrow. except less sucky

/**
 * Repeatedly runs the specified [fn] for [count] times.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <S, F : Fail> repeatedly(count: Int, fn: (Int) -> Either<S, F>): Either<List<S>, F> {
    val items = mutableListOf<S>()
    repeat(count) {
        when (val result = fn(it)) {
            is Ok<S> -> items.add(result.value)
            is Err<F> -> return result
        }
    }

    return Either.ok(items)
}

/**
 * Repeatedly runs the specified [fn] for [count] times or until it fails, either returning a list of successful
 * results or the failure.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <S, F : Fail> repeatedly(
    count: Int,
    fn: (Int) -> CancellableResult<S, F>
): CancellableResult<List<S>, F> {
    val items = mutableListOf<S>()
    repeat(count) {
        when (val result = fn(it)) {
            is Cancelled -> return Cancelled
            is NotCancelled<S, F, Either<S, F>> -> {
                when (result.wrapped) {
                    is Ok<S> -> items.add(result.wrapped.value)
                    is Err<F> -> return result as CancellableResult<List<S>, F>
                }
            }
        }
    }

    return Cancellable.ok(items)
}
