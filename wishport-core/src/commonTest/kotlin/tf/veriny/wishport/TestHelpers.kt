/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTypeInference::class)
package tf.veriny.wishport

import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> Either<S, F>): S {
    val result = fn()
    assertTrue(
        result.isSuccess,
        "should have returned a success, instead got ${result.getFailure()}"
    )
    return result.get()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> Validated<S, F>): S {
    val result = fn()
    assertTrue(
        result.isSuccess,
        "should have returned a success, instead got ${result.getFailures()}"
    )
    return result.get()!!
}


@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> CancellableResult<S, F>): S {
    val result = fn()
    assertTrue(
        result.isSuccess,
        "should have returned a success, instead got $result"
    )
    return result.get()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> CancellableValidated<S, F>): S {
    val result = fn()
    assertTrue(
        result.isSuccess,
        "should have returned a success, instead got $result"
    )
    return result.get()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailure(fn: () -> Either<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure")
    return result.getFailure()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailure(fn: () -> CancellableResult<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure, instead got $result")
    return result.getFailure()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailureWith(f: F, fn: () -> Either<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure $f, instead got $result")
    assertEquals(f, result.getFailure())
    return result.getFailure()!!
}

@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailureWith(f: F, fn: () -> CancellableResult<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure $f, instead got $result")
    assertEquals(f, result.getFailure())
    return result.getFailure()!!
}

inline fun assertCancelled(fn: () -> CancellableResult<*, *>) {
    val result = fn()
    assertTrue(result.isCancelled, "result should have been cancelled, but got $result")
}
