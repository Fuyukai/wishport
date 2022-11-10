package tf.veriny.wishport

import kotlin.experimental.ExperimentalTypeInference

/**
 * Repeatedly runs the specified [fn]. until it results in a failure.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <S, F : Fail> repeatedly(fn: () -> Either<S, F>): Either<*, F> {
    while (true) {
        val result = fn()
        if (result.isFailure) return result
    }
}

/**
 * Repeatedly runs the specified [fn]. until it results in a failure.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <S, F : Fail> repeatedly(
    fn: () -> CancellableResult<S, F>
): CancellableResult<*, F> {
    while (true) {
        val result = fn()
        if (result.isFailure) return result
    }
}