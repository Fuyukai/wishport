/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableEmpty
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.StableApi
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.internals.io.IOManager
import kotlin.coroutines.coroutineContext

@OptIn(LowLevelApi::class)
private fun EventLoop.runWithErrorPrint() {
    try {
        runUntilComplete()
    } catch (e: Throwable) {
        e.printStackTrace()
        throw e
    } finally {
        close()
    }
}

@StableApi
@LowLevelApi
public fun <S, F : Fail> EventLoop.runUntilComplete(
    fn: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    val task = makeRootTask(fn)
    directlyReschedule(task)

    runWithErrorPrint()
    return task.result()
}

/**
 * Runs the specified suspend function until complete.
 */
@LowLevelApi
@StableApi
public fun <S, F : Fail> runUntilComplete(
    clock: Clock? = null,
    fn: suspend () -> CancellableResult<S, F>,
): CancellableResult<S, F> {
    val loop = EventLoop.new(clock)
    return loop.runUntilComplete(fn)
}

/**
 * Runs the specified suspend function until complete. This variant doesn't return a result.
 */
@LowLevelApi
@StableApi
public fun runUntilCompleteNoResult(clock: Clock?, fn: suspend () -> Unit) {
    runUntilComplete(clock) {
        fn(); Cancellable.empty()
    }
}

/**
 * Gets the current task. This is a "fake" suspendable; i.e. it doesn't abide by
 * cancellation or returning an Either.
 */
@LowLevelApi
@StableApi
public suspend inline fun getCurrentTask(): Task {
    try {
        return (coroutineContext as Task.TaskContext).task
    } catch (e: ClassCastException) {
        throw Throwable(
            """
            Something terrible has gone wrong. You cannot use Wishport from within
            the context of another coroutine runner.
            
            If you are seeing this, please make sure that:
            
            1. You are not trying to use Wishport from a ``kotlinx-coroutines`` context
            2. You are not trying to use Wishport from a ``sequence`` context
            
            If neither of the above are true, this is an internal bug and must always
            be reported!
            """.trimIndent(),
            e
        )
    }
}

/**
 * Gets the current I/O manager.
 */
@LowLevelApi
@StableApi
public suspend inline fun getIOManager(): IOManager {
    return EventLoop.get().ioManager
}

/**
 * Reschedules the specified task.
 */
@LowLevelApi
@StableApi
public fun reschedule(task: Task) {
    task.reschedule()
}

/**
 * Waits until this task is rescheduled. This returns an empty cancellation result that can be used
 * for monadic chaining.
 *
 * This is a fragile, low-level API that is prone to failing. For example, this doesn't care if the
 * task is cancelled before suspending, allowing for deadlocks. Any usage of this function should
 * be preceded with a ``checkIfCancelled``, e.g:
 *
 * ```kotlin
 * checkIfCancelled().andThen {
 *     // register some work synchronously, e.g. listening on a socket
 *     val task = getCurrentTask()
 *     registerSomethingThatWillComeBackToUs(task)
 *
 *     /*await*/ waitUntilRescheduled()
 * }
 * ```
 */
@LowLevelApi
@StableApi
public suspend fun waitUntilRescheduled(): CancellableEmpty {
    return getCurrentTask().suspendTask()
}

/**
 * Checks if the current task is cancelled. This is used to avoid doing work
 * (e.g. read() tasks) if the task is cancelled already. Whilst this is a suspend function, this
 * is *not* a suspension point.
 */
@LowLevelApi
@StableApi
public suspend fun checkIfCancelled(): CancellableEmpty {
    val task = getCurrentTask()
    return task.checkIfCancelled()
}
