package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi

/**
 * A Supervisor is an object for spawning long-lived background running tasks with an appropriate
 * error handling strategy.
 *
 * A Supervisor wraps a [Nursery] for structurally concurrent task spawning, and is one of the three
 * primary mechanisms of high-level task spawning in Wishport.
 */
@OptIn(LowLevelApi::class)
public class Supervisor
@PublishedApi internal constructor(private val nursery: Nursery) {
    public companion object {
        /**
         * Starts a new [Supervisor], and passes it to the specified [block]. This function will
         * not return until all child tasks are finished inside the underlying nursery.
         *
         * As [Supervisor]s are designed to last the entire lifetime of a program, the provided
         * lambda shouldn't return anything.
         */
        public suspend inline operator fun invoke(
            crossinline block: suspend (Supervisor) -> Unit
        ) {
            Nursery.open {
                val supervisor = Supervisor(it)
                block(supervisor)
            }
        }
    }

    /**
     * Starts a new long-running background task within this supervisor.
     */
    public suspend fun <S, F : Fail> startBackgroundTask(
        strategy: ErrorHandlingStrategy,
        block: suspend () -> CancellableResult<S, F>
    ): CancellableResult<SupervisorToken, NurseryError> {
        // suppress null cast
        return nursery.start { ts ->
            return@start CancelScope {
                while (true) {
                    val token = SupervisorToken(it)
                    ts.started(token)

                    val result = block()

                    // differentiate between returning Cancelled inside the task (which is annoying
                    // but ideally wouldn't happen)
                    // and actual cancellations.
                    if (it.isEffectivelyCancelled()) {
                        return@CancelScope Cancellable.cancelled()
                    } else {
                        // otherwise, restart the task as appropriate
                        return@CancelScope when (strategy) {
                            ErrorHandlingStrategy.RESTART_ALWAYS -> {
                                continue
                            }
                            ErrorHandlingStrategy.RESTART_ON_ERROR -> {
                                if (result.isFailure) continue
                                else result
                            }
                            ErrorHandlingStrategy.LET_IT_DIE -> {
                                if (result.isFailure) {
                                    nursery.cancelScope.cancel()
                                }
                                result
                            }
                        }
                    }
                }

                // kotlin inference bug, this doesn't compile
                // https://youtrack.jetbrains.com/issue/KT-25023/Infinite-loops-in-lambdas-containing-returnlabel-dont-coerce-to-any-type
                @Suppress("UNREACHABLE_CODE")
                throw Throwable("unreachable")
            }
        } as CancellableResult<SupervisorToken, NurseryError>  // safe cast, we never pass null
    }
}

/**
 * A token returned from [Supervisor.startBackgroundTask] that can be used to cancel individual
 * tasks.
 */
public class SupervisorToken(
    public val scope: CancelScope,
) {
    public var alive: Boolean = true
        private set
}

/**
 * An enumeration of possible error handling strategies.
 */
public enum class ErrorHandlingStrategy {
    /**
     * Always restarts a returning task unless it is cancelled. This will suppress errors except
     * for cancellation.
     */
    RESTART_ALWAYS,

    /**
     * Only restarts a returning task if it returns an error. Otherwise, the task will simply
     * be dropped.
     */
    RESTART_ON_ERROR,

    /**
     * Never restarts a returning task. If it returns an error, then all other tasks in the
     * supervisor will be cancelled; otherwise, the task will simply be dropped.
     */
    LET_IT_DIE,
    ;
}