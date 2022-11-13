/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlin.coroutines.cancellation.CancellationException

/**
 * Allows converting a sequence of functional statements into a sequence of imperative statements
 * via the magic of hidden exceptions.
 */
public class Imperatavize private constructor() {
    @PublishedApi
    internal class MagicFailureException(val f: Fail) : RuntimeException()

    @Suppress("UNCHECKED_CAST")
    public companion object {
        @PublishedApi
        internal val INSTANCE: Imperatavize = Imperatavize()

        public inline fun <S, F : Fail> either(block: Imperatavize.() -> S): Either<S, F> {
            return try {
                Either.ok(block(INSTANCE))
            } catch (m: MagicFailureException) {
                Either.err(m.f as F)
            }
        }

        public suspend inline fun <S, F : Fail> cancellable(
            crossinline block: suspend Imperatavize.() -> S
        ): CancellableResult<S, F> {
            return try {
                Cancellable.ok(block(INSTANCE))
            } catch (m: MagicFailureException) {
                Cancellable.failed(m.f as F)
            } catch (m: CancellationException) {
                Cancellable.cancelled()
            }
        }
    }

    /**
     * Unwraps the value of this [Either] into its success, or causes the entire block to return
     * the Failure.
     */
    public inline fun <S, F : Fail> Either<S, F>.q(): S =
        when (this) {
            is Ok<S> -> value
            is Err<F> -> throw MagicFailureException(value)
        }

    /**
     * Unwraps the value of this [CancellableResult] into its success, or causes the entire block
     * to return either the Cancellable or the Failure.
     */
    public inline fun <S, F : Fail> CancellableResult<S, F>.q(): S =
        when (this) {
            is Cancelled -> throw CancellationException()
            is NotCancelled<S, F, Either<S, F>> -> {
                wrapped.q()
            }
        }
}
