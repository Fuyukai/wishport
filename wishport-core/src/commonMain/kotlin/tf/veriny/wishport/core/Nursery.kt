/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.collections.NonEmptyList
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.sync.Promise

public sealed interface NurseryError : Fail

/** Returned if a task was cancelled during a .start() call. */
public object StartTaskWasCancelled : NurseryError

/** Failure object used to signify this nursery is closed. */
public object NurseryClosed : NurseryError

/**
 * A nursery is a construct that allows for running multiple tasks simultaneously. A nursery is
 * opened by a task, and then new subtasks can be spawned inside that nursery. The core invariant
 * of a Nursery, however, is that the function that opened the nursery will not return until
 * all nursery tasks have completed. This is known as *structured concurrency*.
 */
@OptIn(LowLevelApi::class)
public class Nursery private constructor(private val invokerTask: Task) {
    public companion object {
        @Unsafe
        internal fun unsafeCreate(task: Task) = Nursery(task)

        /**
         * Opens a new Nursery, and calls the specified block with it as the receiver.
         */
        @OptIn(LowLevelApi::class)
        public suspend operator fun <S, F : Fail> invoke(
            block: suspend (Nursery) -> CancellableResult<S, F>
        ): CancellableValidated<S, Fail> {
            val invoker = getCurrentTask()
            val n = Nursery(invoker)

            val result = block(n)

            n.waitForCompletion()

            return if (n.errors.isNotEmpty()) {
                Validated.err(NonEmptyList(n.errors)).notCancelled()
            } else {
                result.validated()
            }
        }
    }

    private val errors = FastArrayList<Fail>()

    /**
     * The cancellation scope associated with this nursery. Cancelling this scope will cancel all
     * tasks within the nursery.
     */
    public val cancelScope: CancelScope = CancelScope.create(invokerTask.context.eventLoop)

    /** If this nursery is open for new children tasks. */
    public var closed: Boolean = false
        private set(value) {
            assert(!field || field == value) { "cannot unclose a nursery" }
            field = value
        }

    init {
        cancelScope.push(invokerTask)
    }

    // if the invoker task is waiting for children, then we wake it up
    // otherwise we leave it alone
    private var suspendedWaitingForChildren = false

    /**
     * The number of direct children tasks currently being ran.
     */
    public var openTasks: Int = 0
        private set

    internal fun taskCompleted(task: Task) {
        openTasks--

        val result = task.result<Any?, Fail>()
        if (result.isCancelled) {
            if (!cancelScope.isEffectivelyCancelled()) {
                // what?
                // TODO: proper logging infra
                println("[WARN] Task $task returned Cancelled from a different cancel scope to ours")
            }
        }

        if (result.isFailure) {
            // uh oh! cancel all other tasks.
            errors.add(result.getFailure()!!)
            cancelScope.cancel()
        }

        // wake up the waiting task if it's waiting for the children
        if (suspendedWaitingForChildren) {
            invokerTask.reschedule(Cancellable.empty())
        }
    }

    @PublishedApi
    internal suspend fun waitForCompletion() {
        suspendedWaitingForChildren = true

        // eat cancellation
        while (openTasks > 0) {
            val result = invokerTask.suspendTask()
            if (result.isCancelled) {
                // outer task is cancelled, all children tasks should *be* cancelled due to them
                // inheriting the nursery's cancel scope.
                // we close anyway
                closed = true
            }
        }

        closed = true
        cancelScope.pop(invokerTask)
        suspendedWaitingForChildren = false
    }

    /**
     * Spawns a new task and schedules it to be ran at the next available opportunity.
     */
    public fun <S, F : Fail> startSoon(
        fn: suspend () -> CancellableResult<S, F>
    ): Either<Unit, NurseryClosed> {
        if (cancelScope.permanentlyCancelled) {
            closed = true
            return Either.err(NurseryClosed)
        }

        if (closed) return Either.err(NurseryClosed)

        val loop = invokerTask.context.eventLoop
        loop.spawnTaskImpl(fn, this)
        openTasks++

        return Either.ok(Unit)
    }

    /**
     * Spawns a new task and waits for it to call [TaskStatus.started]. This will return the value
     * provided, or ``null`` if the task never called it.
     */
    public suspend fun <T, S, F : Fail> start(
        fn: suspend (TaskStatus<T>) -> CancellableResult<S, F>
    ): CancellableResult<T?, NurseryError> {
        if (cancelScope.permanentlyCancelled) {
            closed = true
            return Cancellable.failed(NurseryClosed)
        }
        if (closed) return Cancellable.failed(NurseryClosed)

        val status = TaskStatus<T>()

        // if the function gets cancelled before .started() is called, then we have to signal to
        // the outer task that it got cancelled somehow.
        // we use the StartTaskWasCancelled marker error for this purpose.
        startSoon {
            val result = fn(status)
            if (result.isCancelled) {
                status.taskInternallyWasCancelled = true
            }
            // prevent wait() from dying forever
            status.started(null)
            result
        }

        return status.wait()
    }
}

public class TaskStatus<T> internal constructor() {
    private val event = Promise<T>()

    // XXX: https://github.com/python-trio/trio/issues/2258
    internal var taskInternallyWasCancelled = false

    internal suspend fun wait(): CancellableResult<T?, StartTaskWasCancelled> {
        val result = event.wait()
        return if (taskInternallyWasCancelled) Cancellable.failed(StartTaskWasCancelled)
        else result
    }

    public fun started(data: T?) {
        event.set(data)
    }
}

/**
 * Opens a new nursery without requiring an Either return.
 */
@LowLevelApi
public suspend inline fun Nursery.Companion.open(
    crossinline fn: suspend (Nursery) -> Unit
): CancellableValidated<Unit, Fail> {
    return Nursery { fn(it); checkIfCancelled() }
}

/**
 * Spawns a new task and schedules it to be ran at the next available opportunity.
 *
 * Your function should be returning a CancellableResult, but this is a helper that wraps it safely.
 */
@LowLevelApi
public inline fun Nursery.startSoonNoResult(
    crossinline fn: suspend () -> Unit
): Either<Unit, NurseryClosed> {
    return startSoon {
        Cancellable.ok(fn.invoke())
    }
}
