/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.Nursery
import kotlin.coroutines.coroutineContext

/**
 * Main event loop dispatcher. You almost certainly do not want to use this
 */
@LowLevelApi
public class EventLoop private constructor() {
    public companion object {
        internal fun new() = EventLoop()

        /**
         * Gets the currently running event loop. This will ONLY work from within a Wishport
         * context!
         *
         * This is a fake suspendable; it does not induce a suspension or cancellation point.
         */
        public suspend fun get(): EventLoop {
            return try {
                (coroutineContext as Task.TaskContext).eventLoop
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
    }

    // Task queues:
    // 1) Queue of tasks that are immediately going to run
    private val runningTasks = mutableSetOf<Task>()

    private val rootScope = CancelScope.create(this)
    private lateinit var rootNursery: Nursery

    /**
     * Creates the root task that all tasks inherit from. This will use the root cancellation scope
     * and the root nursery.
     */
    internal fun <S, F : Fail> makeRootTask(coro: suspend () -> CancellableResult<S, F>): Task {
        val task = Task(coro, this, rootScope)
        rootNursery = Nursery(task)
        // make sure everything works out fine
        rootNursery.cancelScope.parent = rootScope
        task.nursery = rootNursery
        return task
    }

    internal fun <S, F : Fail> makeTask(
        coro: suspend () -> CancellableResult<S, F>,
        nursery: Nursery,
    ): Task {
        return Task(coro, this, nursery.cancelScope).also { it.nursery = nursery }
    }

    /**
     * Directly reschedules a task, adding it to the task queue.
     */
    @LowLevelApi
    public fun directlyReschedule(task: Task) {
        runningTasks.add(task)
    }

    /**
     * Runs the event loop until all tasks have been completed.
     */
    public fun runUntilComplete() {
        // TODO: Check for I/O waiting tasks
        // TODO: Check for generally sleepy tasks

        while (runningTasks.isNotEmpty()) {
            // copy to prevent I/O starvation with continuously spawning tasks.
            val copy = runningTasks.toMutableList()
            runningTasks.clear()

            for (task in copy) {
                task.step()
            }
        }
    }
}
