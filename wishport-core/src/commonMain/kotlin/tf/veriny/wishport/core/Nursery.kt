/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTypeInference::class)

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.Task
import kotlin.experimental.ExperimentalTypeInference

/** Failure object used to signify this nursery is closed. */
public object NurseryClosed : Fail

/**
 * A nursery is a construct that allows for running multiple tasks simultaneously. A nursery is
 * opened by a task, and then new subtasks can be spawned inside that nursery. The core invariant
 * of a Nursery, however, is that the function that opened the nursery will not return until
 * all nursery tasks have completed. This is known as *structured concurrency*.
 */
@OptIn(LowLevelApi::class)
public class Nursery @PublishedApi internal constructor(private val invokerTask: Task) {
    public companion object {
        /**
         * Opens a new Nursery, and calls the specified block with it as the receiver.
         */
        @OptIn(LowLevelApi::class)
        public suspend inline operator fun <S, F : Fail> invoke(
            block: (Nursery) -> CancellableResult<S, F>
        ): CancellableResult<S, F> {
            val n = Nursery(getCurrentTask())
            val result = block(n)
            n.waitForCompletion()

            // chain this so that if we get cancelled whilst waiting for the nursery to exit then the
            // result is cancelled.
            // TODO: decide if this is the result we actually want
            return checkIfCancelled().andThen { result }
        }
    }

    /**
     * The cancellation scope associated with this nursery. Cancelling this scope will cancel all
     * tasks within the nursery.
     */
    public val cancelScope: CancelScope = CancelScope.create(invokerTask.context.eventLoop)

    /** If this nursery is open for new children tasks. */
    public var closed: Boolean = false
        private set

    init {
        cancelScope.push(invokerTask)
    }

    // if the invoker task is waiting for children, then we wake it up
    // otherwise we leave it alone
    private var suspendedWaitingForChildren = false

    // used to keep track of the number of direct child subtasks
    private var openTasks = 0

    internal fun taskCompleted(task: Task) {
        // TODO: check result and cancel all tasks on an error

        openTasks--

        // wake up
        if (suspendedWaitingForChildren) {
            invokerTask.context.eventLoop.directlyReschedule(invokerTask)
        }
    }

    @PublishedApi
    internal suspend fun waitForCompletion() {
        suspendedWaitingForChildren = true

        while (openTasks > 0) {
            val result = invokerTask.suspendTask()
            if (result.isCancelled) {
                // we don't care.
            }
        }

        closed = true
        cancelScope.pop(invokerTask)
        suspendedWaitingForChildren = false
    }

    /**
     * Spawns a new task and schedules it to be ran at the next available opportunity.
     */
    @OverloadResolutionByLambdaReturnType
    public fun <S, F : Fail> startSoon(
        fn: suspend () -> CancellableResult<S, F>
    ): Either<Unit, NurseryClosed> {
        if (closed) return Either.err(NurseryClosed)

        val loop = invokerTask.context.eventLoop
        val task = loop.makeTask(fn, this)
        openTasks++
        loop.directlyReschedule(task)

        return Either.ok(Unit)
    }
}

/**
 * Opens a new nursery without requiring an Either return.
 */
public suspend inline fun Nursery.Companion.open(
    crossinline fn: suspend (Nursery) -> Unit
): CancellableEmpty {
    return Nursery { fn(it); Cancellable.empty() }
}

/**
 * Spawns a new task and schedules it to be ran at the next available opportunity.
 *
 * Your function should be returning a CancellableResult, but this is a helper that wraps it safely.
 */
@OverloadResolutionByLambdaReturnType
public inline fun Nursery.startSoonNoResult(
    crossinline fn: suspend () -> Unit
): Either<Unit, NurseryClosed> {
    return startSoon {
        runSafely { fn.invoke() }.notCancelled()
    }
}
