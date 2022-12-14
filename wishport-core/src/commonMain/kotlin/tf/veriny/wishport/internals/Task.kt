/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.Nursery
import kotlin.coroutines.*

// tasks only support a CancellableResult return type
// the spawner functions automatically convert a () -> Unit into a CancellableResult<Unit, Nothing>
// and a () -> Either<S, F> into a CancellableResult<S, F>

// TODO: We can most likely make rescheduling type-safe by having `waitUntilRescheduled` provide
//       a callback with a RescheduleToken<S, F> that needs to be used. Not sure though.

/**
 * Encapsulates the details for an asynchronous task. This is a low-level API primarily used in
 * internals - user code should have no reason to touch this.
 */
@LowLevelApi
public class Task(
    coro: suspend () -> CancellableResult<*, *>,
    private val loop: EventLoop,
    cancelScope: CancelScope,
) : Continuation<CancellableResult<*, *>> {
    /**
     * A special coroutine context that allows accessing the current task and event loop.
     */
    public inner class TaskContext internal constructor() : CoroutineContext by EmptyCoroutineContext {
        public val task: Task get() = this@Task
        public val eventLoop: EventLoop get() = loop
    }

    // allows us to get the event loop from any suspendable function
    override val context: TaskContext = TaskContext()

    // marker variable used inside CancelScope to avoid extra reschedules
    internal var wasRescheduledForCancellation = false

    // used to prevent adding a task to the list multiple times
    internal var wasRescheduledAtAll = false

    // marks this task as the waitUntilAllTasksAreBlocked task. this means it cannot be manually
    // rescheduled.
    internal var isWaitUntilAll = false

    // epic kotlin result type that sucks
    private lateinit var result: CancellableResult<*, *>

    // generator coroutine
    private var continuation = coro.createCoroutine(this)

    // returned by waitUntilRescheduled
    internal var passedInValue: CancellableResult<*, *> = Cancellable.empty()

    internal lateinit var nursery: Nursery

    // public properties
    /** If this task has completed or not. */
    public var finished: Boolean = false
        private set

    /** If this task is the currently running (i.e. primary) task. */
    public var running: Boolean = false
        private set

    /**
     * A custom tag that can be applied to a task before it suspends. This is reset before a task
     * starts stepping again.
     */
    public var customSuspendData: Any? = null

    /** The cancellation scope currently associated with this task. */
    public var cancelScope: CancelScope? = null
        internal set(value) {
            val old = field
            field = value

            old?.tasks?.remove(this)
            value?.tasks?.add(this)
        }

    // https://youtrack.jetbrains.com/issue/KT-6624
    // this downright retarded behaviour caused me like an entire night of anguish
    init {
        this.cancelScope = cancelScope
    }

    override fun resumeWith(result: Result<CancellableResult<*, *>>) {
        if (result.isFailure) {
            // tear down everything, exceptions cannot be handled
            result.getOrThrow()
        }

        this.result = result.intoCancellableResult()
        finished = true
        // disassociate to prevent being rescheduled by the cancel scope
        cancelScope = null
        nursery.taskCompleted(this)

        passedInValue = Cancellable.empty()
        customSuspendData = null
    }

    /**
     * Steps this task once.
     */
    internal fun step() {
        assert(wasRescheduledAtAll) { "Task step() was called without this task being scheduled!" }
        assert(!finished) { "Task has already completed!" }
        assert(cancelScope != null) { "Task stepped without a valid cancel scope!" }

        val lastContinuation = this.continuation
        running = true
        wasRescheduledAtAll = false
        customSuspendData = null

        lastContinuation.resume(Unit)
        // GC helper

        wasRescheduledForCancellation = false
        running = false

        assert(finished || lastContinuation != continuation) {
            "async function stepped, but didn't finish or change continuation!"
        }
    }

    /**
     * Reschedules this task to be ran on the next event loop iteration.
     */
    internal fun reschedule(passedInValue: CancellableResult<*, *>) {
        assert(!finished) { "cannot reschedule a finished task!" }
        assert(!isWaitUntilAll || passedInValue.isCancelled) {
            "you can't manually reschedule the waitUntilAllTasksAreBlocked task!"
        }

        // TODO: this is a low level api but we should probably guard from swapping out
        //       passedInValue from other places.
        this.passedInValue = passedInValue
        loop.directlyReschedule(this)
    }

    /**
     * Suspends the current task.
     */
    internal suspend fun suspendTask(): CancellableResult<Any?, Fail> {
        suspendCoroutine {
            this.continuation = it
        }

        return passedInValue
    }

    /**
     * Gets the result of this task.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <S, F : Fail> result(): CancellableResult<S, F> {
        return result as CancellableResult<S, F>
    }
}

/**
 * Checks if this task is currently cancelled, returning a [CancellableEmpty].
 */
@OptIn(LowLevelApi::class)
public fun Task.checkIfCancelled(): CancellableEmpty {
    return if (cancelScope!!.isEffectivelyCancelled()) {
        Cancellable.cancelled()
    } else {
        Cancellable.empty()
    }
}

/**
 * Causes a checkpoint. This will allow yielding to the event loop for other tasks to do their work
 * before returning here.
 */
@OptIn(LowLevelApi::class)
public suspend fun <S, F : Fail> Task.checkpoint(data: S): CancellableResult<S, F> {
    // checkpoint won't return a Fail ever so this cast is safe.
    return this.checkIfCancelled()
        .andThen {
            // force immediate reschedule
            reschedule(Cancellable.empty())
            suspendTask()
        }
        .andThen {
            Cancellable.ok(data)
        } as CancellableResult<S, F>
}

/**
 * Causes a checkpoint that cannot be cancelled. This will allow yielding to the event loop for
 * other tasks to do their work before returning here.
 */
@OptIn(LowLevelApi::class)
public suspend fun <S> Task.uncancellableCheckpoint(data: S): CancellableSuccess<S> {
    reschedule(Cancellable.empty())
    // eat cancelled
    suspendTask()

    return Cancellable.ok(data)
}
