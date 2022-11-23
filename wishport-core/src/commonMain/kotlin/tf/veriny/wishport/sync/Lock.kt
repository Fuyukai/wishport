/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.sync

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.ParkingLot
import tf.veriny.wishport.core.getCurrentTask
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.internals.uncancellableCheckpoint

// this is non-reentrant. there's a lot of arguments over if locks should be reetrant or not, but
// i'm coming down on the non-reentrant side.
// there's a lot of theoretical arguments being made but I think the easiest argumeent is that they
// encourage bad design.
// see:
// https://github.com/python-trio/trio/issues/1247
// https://blog.stephencleary.com/2013/04/recursive-re-entrant-locks.html

/** Returned if the lock is already owned by the current task. */
public object LockAlreadyOwned : Fail

/**
 * A lock that provides exclusive access to a piece of data.
 */
@OptIn(LowLevelApi::class)
public class Lock<T>(private val data: T) : Closeable {
    private val lot = ParkingLot()
    private var owner: Task? = null

    override val closed: Boolean
        get() {
            return if (data is Closeable) data.closed
            else false
        }

    /**
     * Acquires this lock.
     */
    @PublishedApi
    internal suspend fun acquire(task: Task): CancellableResult<T, LockAlreadyOwned> {
        return task.checkIfCancelled()
            .andThen {
                when (owner) {
                    null -> {
                        owner = task
                        task.uncancellableCheckpoint(Unit)
                    }

                    task -> Cancellable.failed(LockAlreadyOwned)
                    else -> {
                        lot.park(task)
                    }
                }
            }
            .andThen {
                owner = task
                Cancellable.ok(data)
            }
    }

    /**
     * Releases this lock.
     */
    @PublishedApi
    internal fun release(task: Task) {
        // should never ever be violated
        assert(owner == task) { "cannot release lock without being the owner!" }

        owner = null
        lot.unpark(count = 1)
    }

    /**
     * Acquires this lock, providing the specified [block] with the underlying data. The lock
     * will be released once the function completes.
     */
    public suspend inline fun <S, F : Fail> runWhilstLocked(
        crossinline block: suspend (T) -> CancellableResult<S, F>
    ): CancellableResult<S, Fail> {
        val task = getCurrentTask()
        val first = acquire(task)
        val result = first.andThen { block(it) }
        if (first.isSuccess) release(task)
        return result
    }

    override fun close() {
        if (data is Closeable) data.close()
    }
}

// safe cast, as the inner function will only ever return an Ok(Unit) or an Err(LockAlreadyOwned).
@Suppress("UNCHECKED_CAST")
public suspend inline fun <T> Lock<T>.runWhilstLockedNoResult(
    crossinline block: suspend (T) -> Unit
): CancellableResult<Unit, LockAlreadyOwned> {
    return runWhilstLocked {
        block(it); Cancellable.empty()
    } as CancellableResult<Unit, LockAlreadyOwned>
}

// right now this is just lock, but whateveer
/**
 * A lock that operates in a First-in, First-out manner. This guarantees that the task that has
 * been waiting the longest for the lock will be woken up first when the lock is released.
 */
public typealias FIFOLock<T> = Lock<T>
