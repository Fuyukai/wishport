/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.StableApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.AutojumpClock
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.Clock
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.internals.EventLoop.Companion.get
import tf.veriny.wishport.internals.io.IOManager
import tf.veriny.wishport.io.net.DefaultPlatformResolver
import tf.veriny.wishport.io.net.NameResolver
import kotlin.coroutines.coroutineContext

/**
 * Main event loop dispatcher. You almost certainly do not want to use this class directly; instead,
 * use the global helper functions that interface with the event loop behind the scenes.
 *
 * If you need to access the event loop for some reason, you can use [get] from a Wishport context,
 * which will retrieve the event loop variable that is stored implicitly in every asynchronous
 * function's hidden context variable.
 */
@OptIn(Unsafe::class)
@LowLevelApi
@StableApi
public class EventLoop private constructor(
    public val clock: Clock,
    ioManagerSize: Int = -1
) : Closeable {
    public companion object {
        internal fun new(clock: Clock? = PlatformClock): EventLoop {
            val c = clock ?: PlatformClock
            return EventLoop(c)
        }

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

    // set of tasks that are immediately going to run
    private var scheduledTasks = linkedSetOf<Task>()

    internal val deadlines = Deadlines()

    // internal consistency, the root task needs a scope and a nursery
    private val rootScope = CancelScope.create(this, shield = true)
    private lateinit var rootNursery: Nursery

    // used for checking if the loop should exit
    private lateinit var root: Task

    // task that is currently dead on waitAllTasksBlocked
    internal var waitingAllTasksBlocked: Task? = null

    @OptIn(ExperimentalStdlibApi::class)
    private val workerLazy = lazy {
        WorkerPool(this, Platform.getAvailableProcessors())
    }

    // public APIs
    /**
     * The pool of worker threads for running synchronous code off-thread concurrently.
     */
    public val workerPool: WorkerPool by workerLazy

    /**
     * The I/O manager for the current event loop.
     */
    public val ioManager: IOManager = run {
        if (ioManagerSize == -1) IOManager.default()
        else IOManager.withSize(ioManagerSize)
    }

    /**
     * The default DNS resolver to be used by the networking code.
     */
    public var nameResolver: NameResolver = DefaultPlatformResolver

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

    /** Checks for the task waiting for all other tasks to be blocking. */
    private fun checkWaiter(): Boolean {
        val wt = waitingAllTasksBlocked
        if (wt != null) {
            waitingAllTasksBlocked = null
            directlyReschedule(wt)
            return true
        }

        return false
    }

    // I/O methods, with different semantics
    // this one: loop io_uring_peek_cqe
    private fun peekIO() {
        ioManager.pollIO()
    }

    // this one: io_uring_wait_cqes with max queue length and a timeout.
    private fun waitForIO(nextDeadline: Long) {
        if (checkWaiter()) return

        if (clock is AutojumpClock) {
            // we can pretend that the time happened, then peek to see if any I/O wouldve happened
            clock.autojump(nextDeadline)
            peekIO()
        } else {
            val sleepTime = clock.getSleepTime(nextDeadline)
            ioManager.waitForIOUntil(sleepTime)
        }
    }

    // this one: io_uring_wait_cqe. we only wait for one and then peek any remaining ones off
    // to maximise i/o passes for dead tasks
    private fun waitForIOForever() {
        if (checkWaiter()) return

        ioManager.waitForIO()
    }

    // == Public API == //

    /**
     * Directly reschedules a task, adding it to the task queue.
     */
    @LowLevelApi
    @StableApi
    public fun directlyReschedule(task: Task) {
        scheduledTasks.add(task)
    }

    /**
     * Runs the event loop until all tasks have been completed.
     */
    @StableApi
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

        while (true) {
            // pt 1: run any scheduled tasks
            // avoid a copy by using the set in-place and just allocating a new set

            val tasks = this.scheduledTasks
            scheduledTasks = linkedSetOf()

            for (task in tasks) {
                task.step()
            }

            // see above for why this is here
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

            // check for worker thread completion
            if (workerLazy.isInitialized()) {
                workerPool.checkThreads()
            }

            // there's three possible paths here:
            // 1) any tasks waiting, we only peek and never block or else those scheduled tasks will
            //    be waiting for no reason
            // 2) timeout, no tasks waiting, so we need to block for N seconds until the timeout
            //    expires or I/O comes through
            // 3) no timeout, no tasks waiting, so we can just block on I/O forever.

            when {
                scheduledTasks.isNotEmpty() -> {
                    peekIO()
                }
                last != null -> {
                    waitForIO(last.effectiveDeadline)
                }
                else -> {
                    waitForIOForever()
                }
            }
        }

        assert(deadlines.isEmpty()) { "main task exited, but there are still pending scopes!" }
    }

    override fun close() {
        ioManager.close()
    }
}
