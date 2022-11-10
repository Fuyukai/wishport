/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.Task
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
        public const val NEVER_CANCELLED: Long = Long.MAX_VALUE
        public const val ALWAYS_CANCELLED: Long = Long.MIN_VALUE

        // there's a good reason for this to be separated, but I don't remember why...
        @PublishedApi
        internal fun create(loop: EventLoop, shield: Boolean = false): CancelScope {
            return CancelScope(loop, shield = shield)
        }

        /**
         * Creates a new [CancelScope], passing it to the specified function.
         */
        @OptIn(ExperimentalContracts::class)
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

        /**
         * Creates a new [CancelScope], passing it to the specified function that doesn't return
         * a result.
         */
        @OptIn(ExperimentalContracts::class)
        public suspend inline fun open(
            shield: Boolean = false,
            crossinline block: suspend (CancelScope) -> Unit
        ): CancellableEmpty {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }

            return CancelScope(shield = shield) {
                block(it)
                checkIfCancelled()
            }
        }
    }

    override fun compareTo(other: CancelScope): Int {
        return other.effectiveDeadline.compareTo(effectiveDeadline)
    }

    // marks if this cancel scope has been exited or not, prevents stupid shenanigans like returning
    // the scope from the lambda.
    internal var exited: Boolean = false

    @PublishedApi
    internal fun push(task: Task) {
        parent = task.cancelScope
        task.cancelScope = this
    }

    @PublishedApi
    internal fun pop(task: Task) {
        parent?.also { task.cancelScope = it }
        parent = null

        exited = true
        loop.deadlines.remove(this)
    }

    /** True when ``cancel()`` is called. */
    public var cancelCalled: Boolean = false
        private set(value) {
            field = value
            if (value) permanentlyCancelled = true
        }

    /**
     * If this scope is actually permanently cancelled. When True, any attempt at re-entering the
     * event loop will fail.
     */
    public var permanentlyCancelled: Boolean = false
        private set

    // LONG_MAX is always in the future.
    // -LONG_MAX is always in the past, as time is represented by a positive nanosecond monotonic
    // time.
    // it's similar to trio, which uses +inf and -inf, respectively.
    /**
     * The deadline for this cancel scope. This is the absolute number of nanoseconds from an
     * arbitrary point (known as the startup point). This may not be the real deadline; see
     * [effectiveDeadline] for that.
     */
    public var localDeadline: Long = NEVER_CANCELLED
        public set(value) {
            field = value
            quantumObserve()

            // i think this is right...
            if (!permanentlyCancelled) {
                if (value == ALWAYS_CANCELLED) {
                    loop.deadlines.remove(this)
                } else {
                    loop.deadlines.add(this)
                }
            }
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
            if (permanentlyCancelled) return ALWAYS_CANCELLED

            val parent = parent
            if (!shield && parent != null) {
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
            // this is a micro-opt that prevents needing to walk the tree when checking for
            // cancellation.
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
            if (child.shield) continue
            child.permanentlyCancelled = true
            child.dispatchPotentialCancellations()
        }

        for (task in tasks) {
            assert(!task.finished) { "task has finished, but it's still in our task list!" }
            if (task.wasRescheduledForCancellation) continue
            task.wasRescheduledForCancellation = true

            // don't try and reschedule the running task, it'll go badly
            if (task.running) continue
            loop.directlyReschedule(task)
        }
    }
    /**
     * Updates the state of this scope, and all children scopes, as well as dispatching potential
     * reschedules.
     */
    private fun quantumObserve(time: Long) {
        if (!permanentlyCancelled) {
            if (!shield && parent?.isEffectivelyCancelled(time) == true) {
                permanentlyCancelled = true
            } else if (effectiveDeadline <= time) {
                permanentlyCancelled = true
            }
        }

        if (permanentlyCancelled) {
            dispatchPotentialCancellations()
        }
    }

    private fun quantumObserve() = quantumObserve(loop.clock.getCurrentTime())

    // reflector method to let quantumObserve remain private. not sure if this should be the case.
    /**
     * Updates the state from the event loop.
     */
    internal fun updateStateFromEventLoop(time: Long) {
        assert(!exited) { "something has gone terribly wrong!" }

        quantumObserve(time)
    }

    // == public api == //

    /**
     * If true, this scope is shielded from parent cancellations.
     */
    public var shield: Boolean = shield

    /**
     * Permanently cancels this cancel scope.
     */
    public fun cancel() {
        assert(!exited) { "something has gone wrong!" }

        cancelCalled = true
        quantumObserve()
    }

    private fun isEffectivelyCancelled(time: Long): Boolean {
        quantumObserve(time)
        return permanentlyCancelled
    }

    /**
     * Checks if this cancel scope is effectively cancelled.
     */
    public fun isEffectivelyCancelled(): Boolean {
        assert(!exited) { "something has gone wrong!" }

        return isEffectivelyCancelled(loop.clock.getCurrentTime())
    }

    override fun toString(): String {
        return "CancelScope[cancelled=$permanentlyCancelled, " +
            "local=$localDeadline, " +
            "effective=$effectiveDeadline, children=${children.size}]"
    }
}
