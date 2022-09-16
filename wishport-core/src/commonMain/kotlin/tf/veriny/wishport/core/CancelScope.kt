/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.getCurrentTask
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.getMonotonicTime

// unlike trio we unify the cancelscope and cancelstatus logic as i don't really see any reason
// for them to be separated.

/**
 * Encapsulates a single, cancellable unit of code.
 *
 * A [CancelScope] is a level-triggered way of managing cancellation over isolated parts of code.
 * In many cancellation systems, cancellation is all-or-nothing - i.e, at the task level. A
 * [CancelScope] allows cancelling only a specific part of a task, be it with manual cancellation
 * or a timeout.
 *
 * Cancel scopes can only ever transition from the *non-cancelled* to the *cancelled* state; any
 * attempt to uncancel a cancel scope will fail.
 */
@OptIn(LowLevelApi::class)
public class CancelScope private constructor(private val loop: EventLoop) {
    public companion object {
        // there's a good reason for this to be separated, but I don't remember why...
        @PublishedApi
        internal fun create(loop: EventLoop): CancelScope {
            return CancelScope(loop)
        }

        /**
         * Creates a new [CancelScope], passing it to the specified function.
         */
        public suspend inline operator fun <S, F : Fail> invoke(
            crossinline block: suspend (CancelScope) -> CancellableResult<S, F>
        ): CancellableResult<S, F> {
            val task = getCurrentTask()
            val newScope = create(task.context.eventLoop)
            newScope.push(task)

            val result = block(newScope)

            newScope.pop(task)

            return result
        }
    }

    @PublishedApi
    internal fun push(task: Task) {
        parent = task.cancelScope
        task.cancelScope = this
    }

    @PublishedApi
    internal fun pop(task: Task) {
        parent?.also { task.cancelScope = it }
        parent = null
    }

    /** True when ``cancel()`` is called. */
    public var cancelCalled: Boolean = false
        private set(value) {
            field = value
            if (value) permanentlyCancelled = true
        }

    // internal usage, used to prevent child cancel scopes from being uncancelled.
    private var permanentlyCancelled: Boolean = false

    // LONG_MAX is always in the future.
    // -LONG_MAX is always in the past, as time is represented by a positive nanosecond monotonic
    // time.
    // it's similar to trio, which uses +inf and -inf, respectively.
    /**
     * The deadline for this cancel scope. This is the absolute number of nanoseconds from an
     * arbitrary point (known as the startup point).
     */
    public var deadline: Long = Long.MAX_VALUE
        get() {
            return if (permanentlyCancelled) -Long.MAX_VALUE
            else field
        }

        public set(value) {
            field = value
            dispatchPotentialCancellations()
        }

    /**
     * The parent [CancelScope] for this scope.
     */
    public var parent: CancelScope? = null
        internal set(value) {
            val oldParent = field
            field = value

            oldParent?.children?.remove(this)
            value?.children?.add(this)
        }

    // delivered
    private val children = mutableSetOf<CancelScope>()
    internal val tasks = mutableSetOf<Task>()

    /**
     * Dispatches potential cancellations to children tasks and cancel scopes.
     */
    private fun dispatchPotentialCancellations() {
        if (!isEffectivelyCancelled()) return

        for (task in tasks) {
            if (task.running) continue // don't try and reschedule the running task
            loop.directlyReschedule(task)
        }
    }

    /**
     * If true, this scope is shielded from parent cancellations.
     */
    public var shield: Boolean = false

    /**
     * Checks if this cancel scope is effectively cancelled.
     */
    public fun isEffectivelyCancelled(): Boolean {
        if (!shield && parent != null) {
            if (parent!!.isEffectivelyCancelled()) {
                return true
            }
        }

        return permanentlyCancelled || deadline < getMonotonicTime()
    }

    /**
     * Permanently cancels this cancel scope.
     */
    public fun cancel() {
        cancelCalled = true
        dispatchPotentialCancellations()
    }
}
