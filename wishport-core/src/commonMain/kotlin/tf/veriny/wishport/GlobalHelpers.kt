/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.Clock
import tf.veriny.wishport.internals.*
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

/**
 * Runs the specified suspend function until complete.
 */
@OptIn(LowLevelApi::class)
public fun <S, F : Fail> runUntilComplete(
    clock: Clock? = null,
    fn: suspend () -> CancellableResult<S, F>,
): CancellableResult<S, F> {
    val loop = EventLoop.new(clock)
    val task = loop.makeRootTask(fn)
    loop.directlyReschedule(task)

    loop.runWithErrorPrint()
    return task.result()
}

// workaround for weird optin stuff
/**
 * Variant of [runUntilComplete] that doesn't require a [Clock].
 */
@OptIn(LowLevelApi::class)
public fun <S, F : Fail> runUntilComplete(
    fn: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    return runUntilComplete(null, fn)
}

// XXX: Kotlin forces this overload no matter what so it's hackily renamed.
// Not sure why this is the case.
/**
 * Runs the specified suspend function until complete. This variant doesn't return a result.
 */
@LowLevelApi
public fun runUntilCompleteNoResult(clock: Clock?, fn: suspend () -> Unit) {
    val loop = EventLoop.new(clock)
    val task = loop.makeRootTask {
        fn(); Cancellable.empty()
    }

    loop.directlyReschedule(task)
    loop.runWithErrorPrint()
}

// workaround for weird optin stuff
/**
 * Variant of [runUntilCompleteNoResult] that doesn't require a [Clock].
 */
@OptIn(LowLevelApi::class)
public fun runUntilCompleteNoResult(fn: suspend () -> Unit) {
    runUntilCompleteNoResult(null, fn)
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
 * Gets the current I/O manager.
 */
@Unsafe
@LowLevelApi
public suspend inline fun getIOManager(): IOManager {
    return EventLoop.get().ioManager
}

/**
 * Reschedules the specified task.
 */
@LowLevelApi
public fun reschedule(task: Task) {
    task.reschedule()
}

/**
 * Gets the current time according to the event loop, in nanoseconds. This is a "fake" suspendable,
 * i.e. it doesn't abide by cancellation or returning an Either.
 */
@OptIn(LowLevelApi::class)
public suspend fun getCurrentTime(): Long {
    return EventLoop.get().clock.getCurrentTime()
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
    return task.checkIfCancelled()
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
    val task = getCurrentTask()
    return task.checkpoint(value)
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

/**
 * Causes a checkpoint that cannot be cancelled. This will allow yielding to the event loop for
 * other tasks to do their work before returning here.
 *
 * This is used to enforce Wishport's cancellation semantics; either a cancellation happened, or
 * the operation completed. A regular checkpoint can cause a cancellation even if the operation
 * happened, leading to inconsistent state.
 */
@OptIn(LowLevelApi::class)
public suspend fun <S> uncancellableCheckpoint(value: S): CancellableSuccess<S> {
    val task = getCurrentTask()
    return task.uncancellableCheckpoint(value)
}

/**
 * Like [uncancellableCheckpoint], but this doesn't take a value.
 */
@OptIn(LowLevelApi::class)
public suspend fun uncancellableCheckpoint(): CancellableEmpty {
    return uncancellableCheckpoint(Unit)
}

/**
 * Waits until all other tasks are currently blocked (waiting to be rescheduled). This will
 * ONLY fire if there are no possible other tasks that can run on the next iteration of
 * the event loop.
 */
@OptIn(LowLevelApi::class)
public suspend fun waitUntilAllTasksAreBlocked(): CancellableEmpty {
    val task = getCurrentTask()
    val loop = task.context.eventLoop

    return task.checkIfCancelled()
        .andThen {
            loop.waitingAllTasksBlocked = task
            task.suspendTask()
        }
}

// == Sleep/Timeout Helpers == //
// structured similarly to trio
/**
 * Runs the specified [block] with a timeout of [nanos] nanoseconds. If the actions within do not
 * complete in time, then the function is cancelled.
 */
public suspend inline fun <S, F : Fail> moveOnAfter(
    nanos: Long,
    crossinline block: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    return CancelScope { cs ->
        cs.localDeadline = getCurrentTime() + nanos
        block()
    }
}

/**
 * Runs the specified [block], timing out at the exact time specified by [nanos]. If the actions
 * within do not complete in time, then the function is cancelled.
 *
 * If the time specified is in the past, then this will perform a checkpoint, and all results inside
 * the block will be cancelled.
 */
public suspend inline fun <S, F : Fail> moveOnAt(
    nanos: Long,
    crossinline block: suspend () -> CancellableResult<S, F>
): CancellableResult<S, F> {
    // note: don't need to return a cancelled here (imo) as it's less surprising the code within the
    // block gets executed, but always returns cancelled.
    // we do want to perform a checkpoint to allow yielding.
    if (nanos < getCurrentTime()) {
        checkpoint()
    }

    return CancelScope { cs ->
        cs.localDeadline = nanos
        block()
    }
}

/**
 * Sleeps forever (or, until cancelled).
 */
@OptIn(LowLevelApi::class)
public suspend fun sleepForever(): CancellableEmpty {
    return waitUntilRescheduled()
}

/**
 * Sleeps until the specified time, in nanoseconds.
 */
@OptIn(LowLevelApi::class)
public suspend fun sleepUntil(time: Long): CancellableEmpty {
    moveOnAt(time) { sleepForever() }
    return checkIfCancelled()
}

/**
 * Sleeps for the specified amount of nanoseconds, returning an empty value.
 */
@OptIn(LowLevelApi::class)
public suspend fun sleep(nanos: Long): CancellableEmpty {
    return sleepUntil(getCurrentTime() + nanos)
}
