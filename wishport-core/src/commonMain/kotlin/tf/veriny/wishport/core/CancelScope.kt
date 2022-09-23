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

// unlike trio we unify the cancelscope and cancelstatus logic as i don't really see any reason
// for them to be separated.

// Cancellation can be considered a sort of quantum state; that is, a scope is only cancelled once
// it is observed to be cancelled. Until then, it's unknowable if the scope is actually cancelled.
//
// The ways a scope can be cancelled are:
// 1) If ``cancel()`` is explicitly called on a scope, or its parent scopes.
// 2) If the ``localDeadline`` property is edited, and either a parent scope is already cancelled,
//    or the ``localDeadline`` provided is in the past.
// 3) When ``isEffectivelyCancelled()``, and the parent cancel scope is also cancelled.

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
@Suppress("CanBePrimaryConstructorProperty")
@OptIn(LowLevelApi::class)
public class CancelScope
private constructor(
    private val loop: EventLoop,
    shield: Boolean = false,
) : Comparable<CancelScope> {
    public companion object {
        // there's a good reason for this to be separated, but I don't remember why...
        @PublishedApi
        internal fun create(loop: EventLoop, shield: Boolean = false): CancelScope {
            return CancelScope(loop, shield = shield)
        }

        /**
         * Creates a new [CancelScope], passing it to the specified function.
         */
        public suspend inline operator fun <S, F : Fail> invoke(
            shield: Boolean = false,
            crossinline block: suspend (CancelScope) -> CancellableResult<S, F>
        ): CancellableResult<S, F> {
            val task = getCurrentTask()
            val newScope = create(task.context.eventLoop, shield)
            newScope.push(task)

            val result = block(newScope)

            newScope.pop(task)

            return result
        }
    }

    override fun compareTo(other: CancelScope): Int {
        return other.effectiveDeadline.compareTo(effectiveDeadline)
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
     * arbitrary point (known as the startup point). This may not be the real deadline; see
     * [effectiveDeadline] for that.
     */
    public var localDeadline: Long = Long.MAX_VALUE
        public set(value) {
            field = value
            quantumObserve()
        }

    /**
     * The effective deadline for this cancel scope. This can be different from the real deadline
     * in a handful of situations:
     *
     *  1. When the parent deadline is before us
     *  2. If we are cancelled (then this will be Long.MIN_VALUE)
     */
    public val effectiveDeadline: Long
        get() {
            if (permanentlyCancelled) return Long.MIN_VALUE

            val parent = parent
            if (parent != null) {
                if (parent.effectiveDeadline < localDeadline) return parent.effectiveDeadline
            }

            return localDeadline
        }

    // XXX: this should only be set once (at creation)
    //      i think theoretically this does support re-parenting but don't do it.
    /**
     * The parent [CancelScope] for this scope.
     */
    public var parent: CancelScope? = null
        internal set(value) {
            val oldParent = field
            field = value

            oldParent?.children?.remove(this)
            value?.children?.add(this)

            // only happens in weird cases, such as opening a new cancel scope, cancelling it,
            // then immediately opening a child one.
            // this prevents the inner scope from
            if (!shield && value?.permanentlyCancelled == true) {
                permanentlyCancelled = true
            }
        }

    internal val children = mutableSetOf<CancelScope>()
    internal val tasks = mutableSetOf<Task>()

    private fun dispatchPotentialCancellations() {
        // guard
        if (!permanentlyCancelled) return

        for (child in children) {
            child.quantumObserve()
        }

        for (task in tasks) {
            // don't try and reschedule the running task, it'll go badly
            if (task.running) continue
            loop.directlyReschedule(task)
        }
    }

    /**
     * Updates the state of this scope, and all children scopes, as well as dispatching potential
     * reschedules.
     */
    private fun quantumObserve() {
        if (!permanentlyCancelled) {
            if (!shield && parent?.isEffectivelyCancelled() == true) {
                permanentlyCancelled = true
            }

            else if (effectiveDeadline < loop.clock.getCurrentTime()) {
                permanentlyCancelled = true
            }
        }

        if (permanentlyCancelled) {
            dispatchPotentialCancellations()
        }
    }

    /**
     * If true, this scope is shielded from parent cancellations.
     */
    public var shield: Boolean = shield

    /**
     * Permanently cancels this cancel scope.
     */
    public fun cancel() {
        cancelCalled = true
        quantumObserve()
    }

    /**
     * Checks if this cancel scope is effectively cancelled.
     */
    public fun isEffectivelyCancelled(): Boolean {
        quantumObserve()

        return permanentlyCancelled
    }

}
