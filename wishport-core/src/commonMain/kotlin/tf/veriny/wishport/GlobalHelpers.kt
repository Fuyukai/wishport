/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.Task
import kotlin.coroutines.coroutineContext

@OptIn(LowLevelApi::class)
private fun EventLoop.runWithErrorPrint() {
    try {
        runUntilComplete()
    } catch (e: Throwable) {
        e.printStackTrace()
        throw e
    }
}

/**
 * Runs the specified suspend function until complete.
 */
@OptIn(LowLevelApi::class)
public fun <S, F : Fail> runUntilComplete(
    fn: suspend () -> CancellableResult<S, F>,
): CancellableResult<S, F> {
    val loop = EventLoop.new()
    val task = loop.makeRootTask(fn)
    loop.directlyReschedule(task)

    loop.runWithErrorPrint()
    return task.result()
}

// XXX: Kotlin forces this overload no matter what so it's hackily renamed.
// Not sure why this is the case.
/**
 * Runs the specified suspend function until complete. This variant doesn't return a result.
 */
@OptIn(LowLevelApi::class)
public fun runUntilCompleteNoResult(fn: suspend () -> Unit) {
    val loop = EventLoop.new()
    val task = loop.makeRootTask {
        fn(); Cancellable.empty()
    }

    loop.directlyReschedule(task)
    loop.runWithErrorPrint()
}

/**
 * Gets the current task. This is a "fake" suspendable; i.e. it doesn't abide by
 * cancellation or returning an Either.
 */
@LowLevelApi
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
 * Reschedules the specified task.
 */
@LowLevelApi
public fun reschedule(task: Task) {
    task.context.eventLoop.directlyReschedule(task)
}

/**
 * Gets the current time according to the event loop, in nanoseconds. This is a "fake" suspendable,
 * i.e. it doesn't abide by cancellation or returning an Either.
 */
@OptIn(LowLevelApi::class)
public suspend fun getCurrentTime(): Long {
    return getCurrentTask().context.eventLoop.clock.getCurrentTime()
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
 *
 * You probably want [checkpoint] instead.
 */
@LowLevelApi
public suspend fun waitUntilRescheduled(): CancellableEmpty {
    return getCurrentTask().suspendTask()
}

/**
 * Checks if the current task is cancelled. This is used to avoid doing work
 * (e.g. read() tasks) if the task is cancelled already. Whilst this is a suspend function, this
 * is *not* a suspension point.
 */
@LowLevelApi
public suspend fun checkIfCancelled(): CancellableEmpty {
    val task = getCurrentTask()
    return if (task.cancelScope!!.isEffectivelyCancelled()) {
        Cancellable.cancelled()
    } else {
        Cancellable.empty()
    }
}

/**
 * Causes a checkpoint. This will allow yielding to the event loop for other tasks to do their work
 * before returning here.
 *
 * This variant allows passing a value for easier monad chaining, e.g.:
 *
 * ```kotlin
 *
 * return someOperation().andThen { checkpoint(it) }
 *
 * ```
 */
@OptIn(LowLevelApi::class)
public suspend fun <S, F : Fail> checkpoint(value: S): CancellableResult<S, F> {
    return checkIfCancelled()
        .andThen {
            // force immediate reschedule
            val task = getCurrentTask()
            reschedule(task)
            task.suspendTask()
        }
        .andThen {
            Cancellable.notCancelled(value)
        }
}

/**
 * Causes a checkpoint. This will allow yielding to the event loop for other tasks to do their work
 * before returning here.
 *
 * This variant does not have a value.
 */
public suspend fun checkpoint(): CancellableEmpty {
    return checkpoint(Unit)
}
