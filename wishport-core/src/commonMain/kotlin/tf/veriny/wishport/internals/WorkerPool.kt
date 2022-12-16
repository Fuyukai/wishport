/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.ParkingLot
import tf.veriny.wishport.core.getCurrentTask
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.FutureState
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

// several problems here. future doesn'tt have a callback mechanism so we can't easily
// wake up the calling task. lucky, we can do this in a relatively concurrency safe way
// we add a new method to iomanager that forcibly wakes up a blocked call (timeout, or otherwise)
// e.g. using an eventfd on linux.
// this method is synchronous and just does a write(2) to the eventfd which is fine as it's an
// off-thread system call.
// we have a permanent multishot poll, which is edge triggered, over the eventfd for writeability
// this special cqe is ignored inside the actual polling loop.
// then, the event loop checks every loop iteration for if off-thread tasks are completed w/ simple
// iteration over the list of futures. this prevents any annoying interleaving with successive
// write calls to eventfd not going through properly.

@LowLevelApi
public class WorkerPool(public val loop: EventLoop, public val size: Int) {
    private class WorkerJobState<I, R>(
        val loop: EventLoop,
        val res: I,
        val callback: (I) -> R,
    )

    internal class WorkerFuture<T>(
        val worker: Worker,
        val future: Future<T>,
        val task: Task,
        var dead: Boolean = false,
    )

    private val workers = ArrayDeque<Worker>()
    private val futures = mutableListOf<WorkerFuture<*>>()
    private val waiters = ParkingLot()

    private var nameSeq = 0

    init {
        for (i in 0 until size) {
            workers.add(Worker.start(name = "Wishport-WorkerThread-${++nameSeq}"))
        }
    }

    /**
     * Checks for completed tasks.
     */
    internal fun checkThreads() {
        val iterator = futures.listIterator()

        for (fut in iterator) {
            if (fut.future.state == FutureState.COMPUTED) {
                fut.task.reschedule(Cancellable.empty())
                iterator.remove()

                if (!fut.dead) {
                    workers.addLast(fut.worker)
                } else {
                    val newWorker = Worker.start(name = "Wishport-WorkerThread-${++nameSeq}")
                    workers.addLast(newWorker)
                }
                waiters.unpark(1)
            }
        }
    }

    /**
     * Runs a function synchronously in another thread.
     */
    public suspend fun <I, S, F : Fail> runSyncInThread(
        cancellable: Boolean,
        producer: () -> I,
        block: (I) -> Either<S, F>,
    ): CancellableResult<S, F> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                if (workers.isEmpty()) {
                    waiters.park(task)
                } else {
                    Cancellable.empty()
                }
            }
            .andThen {
                val nextWorker = workers.removeFirst()

                val future = nextWorker.execute(
                    TransferMode.SAFE,
                    // gross capturing setup
                    { WorkerJobState(loop, producer(), block) },
                    {
                        val loop = it.loop
                        val result = it.callback(it.res)
                        loop.ioManager.forceWakeUp()
                        result
                    }
                )

                val fut = WorkerFuture(nextWorker, future, task)
                futures.add(fut)

                while (future.state == FutureState.SCHEDULED) {
                    val res = task.suspendTask()
                    if (res.isCancelled && cancellable) {
                        // abandon the worker to do whatever
                        nextWorker.requestTermination()
                        fut.dead = true
                        return@andThen Cancellable.cancelled()
                    }
                }

                return@andThen fut.future.result.notCancelled()
            }
    }
}
