package tf.veriny.wishport

import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.collections.NonEmptyList
import tf.veriny.wishport.collections.nonEmptyListOf
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// == tf.veriny.wishport.validated helpers == //
/**
 * Converts this [Either] into a [Validated].
 */
public inline fun <S, F : Fail> Either<S, F>.validated(): Validated<S, F> =
    when (this) {
        is Ok<S> -> this
        is Err<F> -> MultiErr<F>(nonEmptyListOf(value))
    }

/**
 * Gets the [List] of errors that this [Validated] wraps. If this is a success, then the list will
 * be empty. Otherwise, the list is guaranteed to be non-empty.
 */
public inline fun <F : Fail> Validated<*, F>.getFailures(): List<F> =
    when (this) {
        is Ok<*> -> emptyList()
        is MultiErr<F> -> failures
    }

/**
 * Folds a [Validated], either passing the successful value [S] to the [success] block provided, or
 * a [NonEmptyList] of error values [F] to the [fail] block provided.
 */
public inline fun <Out, S, F : Fail> Validated<S, F>.fold(
    success: (S) -> Out,
    fail: (NonEmptyList<F>) -> Out,
): Out =
    when (this) {
        is Ok<S> -> success(value)
        is MultiErr<F> -> fail(failures)
    }

// horrific gross code!
/**
 * A helper scope that lets you write [Validated] code in a more imperative way.
 *
 * An example:
 * ```kotlin
 * val parsed = form.parse().q()
 * val validated = ValidatedScope {
 *    val username by validated { usernameValidator.get(parsed) }
 *    val email by validated { emailValidator.get(email) }
 *
 *    fullyValidate { User(username, email) }
 * }
 *
 */
@OptIn(ExperimentalTypeInference::class)
public class ValidatedScope<S : Any, F : Fail>
@PublishedApi internal constructor() {
    public companion object {
        public inline fun <S : Any, F : Fail> invoke(
            block: ValidatedScope<S, F>.() -> Validated<S, Fail>
        ): Validated<S, Fail> {
            val scope = ValidatedScope<S, F>()
            return scope.block()
        }
    }

    private class UsedInvalidValidated(public val what: String) : Fail
    private class MagicControlFlow(val error: Fail) : Throwable()

    private inner class ValidatedProperty<S : Any, F : Fail>(
        val fn1: (() -> Either<S, F>)?,
        val fn2: (() -> Validated<S, F>)?,
    ) : ReadOnlyProperty<Any?, S> {
        var errored = false
        lateinit var value: S

        override fun getValue(thisRef: Any?, property: KProperty<*>): S {
            if (errored) throw MagicControlFlow(UsedInvalidValidated(property.name))
            return value
        }
    }

    private val properties = FastArrayList<ValidatedProperty<S, F>>()

    @OverloadResolutionByLambdaReturnType
    public fun validated(block: () -> Either<S, F>): ReadOnlyProperty<*, S> {
        val prop = ValidatedProperty(fn1 = block, fn2 = null)
        properties.add(prop)
        return prop
    }

    @OverloadResolutionByLambdaReturnType
    public fun validated(block: () -> Validated<S, F>): ReadOnlyProperty<*, S> {
        val prop = ValidatedProperty(fn1 = null, fn2 = block)
        properties.add(prop)
        return prop
    }

    public fun fullyValidate(block: () -> Either<S, F>): Validated<S, Fail> {
        val errors = FastArrayList<Fail>()
        for (i in properties) {
            try {
                when {
                    i.fn1 != null -> {
                        when (val result = i.fn1.invoke()) {
                            is Ok<S> -> i.value = result.value
                            is Err<F> -> errors.add(result.value)
                        }
                    }

                    i.fn2 != null -> {
                        when (val result = i.fn2.invoke()) {
                            is Ok<S> -> i.value = result.value
                            is MultiErr<F> -> errors.addAll(result.failures)
                        }
                    }
                }
            } catch (e: MagicControlFlow) {
                errors.add(e.error)
            }
        }

        return if (errors.isNotEmpty()) {
            MultiErr(NonEmptyList(errors))
        } else {
            block().validated()
        }
    }
}