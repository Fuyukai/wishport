/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> Either<S, F>): S {
    val result = fn()
    assertTrue(result.isSuccess, "should have returned a success")
    return result.get()!!
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <S, F : Fail> assertSuccess(fn: () -> CancellableResult<S, F>): S {
    val result = fn()
    assertTrue(
        result.isSuccess,
        "should have returned a success, instead got ${result.getFailure()}"
    )
    return result.get()!!
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailure(fn: () -> Either<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure")
    return result.getFailure()!!
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailure(fn: () -> CancellableResult<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure")
    return result.getFailure()!!
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailureWith(f: F, fn: () -> Either<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure")
    assertEquals(f, result.getFailure())
    return result.getFailure()!!
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <F : Fail> assertFailureWith(f: F, fn: () -> CancellableResult<*, F>): F {
    val result = fn()
    assertTrue(result.isFailure, "should have returned failure")
    assertEquals(f, result.getFailure())
    return result.getFailure()!!
}
