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
import tf.veriny.wishport.core.Clock
import tf.veriny.wishport.core.Nursery
import kotlin.coroutines.coroutineContext

/**
 * Main event loop dispatcher. You almost certainly do not want to use this
 */
@LowLevelApi
public class EventLoop private constructor(public val clock: Clock) {
    public companion object {
        internal fun new(clock: Clock = PlatformClock) = EventLoop(clock)

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
    // 1) Set of tasks that are immediately going to run
    private val runningTasks = mutableSetOf<Task>()

    internal val deadlines = Deadlines()

    private val rootScope = CancelScope.create(this)
    private lateinit var rootNursery: Nursery

    private lateinit var root: Task

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
        root = task
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
        // This is structured peculiarly, but for a good reason.
        //
        // If we check for the root task's finished status too early, we won't know it's finished
        // until after we go to I/O, and if there's nothing pending, we'll block forever in
        // io_uring_wait_cqe with nothing coming.
        //
        // So instead, we check directly after all rescheduled tasks have completed.
        // If it's completed, then we can exit and then assert a few invariants to make sure all
        // the code is correct.
        //
        // Otherwise, we go to check cancellations. If there *are* cancellations, then the relevant
        // tasks will be rescheduled, and I/O will be eagerly polled but not blocked on, allowing
        // the freshly cancelled tasks to be rescheduled later.
        //
        // If there are no cancellations, and the root task is NOT finished, then we can safely
        // block in I/O wait forever until something comes in - there's nothing to run.
        //
        // If this is ordered differently, then the loop will deadlock on I/O forever despite
        // the main task (and thus all children tasks) having been completed. I do have a potential
        // safety check for this (wrap the main task with opening a pipe, registering it for read,
        // closing it at exit, and then making the io_uring loop fail if there's no SQEs) but I
        // would rather have the event loop code work than this.
        // In the future we may need internal pipes, so this could be revisited as a mechanism
        // anyway, as an additional safety check.

        // TODO: Check for I/O waiting tasks

        while (true) {
            // pt 1: run any scheduled tasks
            // copy to prevent I/O starvation with continuously spawning tasks.
            val copy = runningTasks.toMutableList()
            runningTasks.clear()

            for (task in copy) {
                task.step()
            }

            if (root.finished) break

            // pt 2: gather up expired deadlines and update their state
            val currentTime = clock.getCurrentTime()
            val (expired, last) = deadlines.purge(currentTime)

            // this will cause every scope to go "uh oh", realise its cancelled, and update its
            // permanentlyCancelled var. then itll loop through any children and wait for them to
            // finish.
            for (e in expired) {
                e.updateStateFromEventLoop(currentTime)
            }

            // TODO: actually block on our I/O manager
            if (last == null) {
                if (runningTasks.isEmpty()) {
                    TODO("block on I/O forever")
                } else {
                    // retrieve i/o events without waiting, i.e. loop io_uring_peek_cqe
                }
            } else {
                val sleepTime = clock.getSleepTime(last.effectiveDeadline)
                nanosleep(sleepTime)
            }
        }

        assert(deadlines.isEmpty()) { "main task exited, but there are still pending scopes!" }
    }
}
