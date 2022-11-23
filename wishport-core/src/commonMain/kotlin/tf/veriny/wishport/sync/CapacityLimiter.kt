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

// like a semaphore, but per-task rather than based on arbitrary numbers.

/**
 * Returned if the current task tries to acquire the capacity limiter, but it already holds a token.
 */
public object AlreadyAcquired : Fail

/**
 * An object that controls access to a limited resource. This will only allow N tasks to unwrap the
 * data held within.
 */
@OptIn(LowLevelApi::class)
public class CapacityLimiter<T>(
    tokens: Int,
    private val data: T,
) : Closeable {
    override val closed: Boolean
        get() {
            return if (data is Closeable) data.closed
            else false
        }

    /** The number of tokens available in this capacity limiter. */
    public var totalTokens: Int = tokens
        private set

    /** The current number of tokens available. */
    public var currentTokens: Int = totalTokens
        private set

    private val borrowers = mutableSetOf<Task>()
    private val lot = ParkingLot()

    @PublishedApi
    internal suspend fun acquire(task: Task): CancellableResult<T, AlreadyAcquired> {
        // racy conditions, so we can't use the monadic stuff here unfortunately.
        if (task.checkIfCancelled().isCancelled) return Cancellable.cancelled()
        if (task in borrowers) return Cancellable.failed(AlreadyAcquired)

        // if there's tokens available, decrement then checkpoint.
        // if we checkpointed then decremented (as in my first impl) we'll fail if more than N
        // tasks acquire simultaneously
        if (currentTokens > 0) {
            currentTokens--
            borrowers.add(task)
            return task.uncancellableCheckpoint(data)
        }

        // no tokens, park then decrement once rescheduled synchronously
        // this is done in lockstep with release (1 release = 1 reschedule) so this is
        // always fine.
        return lot.park()
            .andThen {
                assert(currentTokens > 0) { "i fucked up the race condition again!" }

                currentTokens--
                borrowers.add(task)
                Cancellable.ok(data)
            }
    }

    @PublishedApi
    internal fun release(task: Task) {
        currentTokens += 1
        borrowers.remove(task)
        lot.unpark(count = 1)
    }

    /**
     * Unwraps the data within this limiter, and invokes the passed in [block] with it. If there
     * are no tokens available this will block until another task releases the limiter.
     */
    public suspend inline fun <S, F : Fail> unwrapAndRun(
        crossinline block: suspend (T) -> CancellableResult<S, F>
    ): CancellableResult<S, Fail> {
        val task = getCurrentTask()
        val check = acquire(task)
        val res = check.andThen { block(it) }
        if (check.isSuccess) release(task)
        return res
    }

    override fun close() {
        if (data is Closeable) data.close()
    }
}
