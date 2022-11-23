/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.channel

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.core.getCurrentTask
import tf.veriny.wishport.core.waitUntilRescheduled
import tf.veriny.wishport.helpers.TaskList
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.internals.uncancellableCheckpoint

// requires a bit of annoying synchronisation, this can probably be deduped a buncch.
// invariant: if a task is rescheduled with a non-cancellation result you mustn't clean up your
// data.

/**
 * Wraps the statistics for a memory channel.
 */
public data class MemoryChannelStatistics(
    /** The number of send channels currently connected to this channel. */
    public val sendChannelCount: Int,
    /** The number of receive channels currently connected to this channel. */
    public val receiveChannelCount: Int,
    /** The number of tasks waiting to send some data over this channel. */
    public val sendWaiters: Int,
    /** The number of tasks waiting to receive data from this channel. */
    public val receiveWaiters: Int,
    /** The buffer size for this channel. */
    public val bufferSize: Int,
    /** The number of items currently buffered for this channel. */
    public val bufferedItems: Int,
)

/**
 * Handles internal details of the state between two channels.
 */
@OptIn(LowLevelApi::class)
internal class MemoryChannelState<E : Any>(private val bufferSize: Int) {
    // TODO: use our own deque.
    private val buffer = ArrayDeque<E>()

    private val waitingData = mutableMapOf<Task, E>()
    private val waitingSends = TaskList<Task>()

    private val waitingReceives = TaskList<Task>()

    internal var sendChannelCount = 0
    internal var receiveChannelCount = 0

    internal var closed = false

    internal fun statistics(): MemoryChannelStatistics {
        return MemoryChannelStatistics(
            sendChannelCount, receiveChannelCount,
            waitingSends.size, waitingReceives.size,
            bufferSize, buffer.size
        )
    }

    private fun teardown() {
        sendChannelCount = 0
        receiveChannelCount = 0
        closed = true

        for (task in waitingSends) {
            task.reschedule(Cancellable.failed(AlreadyClosedError))
        }
        for (task in waitingReceives) {
            task.reschedule(Cancellable.failed(AlreadyClosedError))
        }

        waitingSends.clear()
        waitingReceives.clear()
        waitingData.clear()
    }

    internal fun closeSendChannel(channel: MemorySendChannel<E>) {
        sendChannelCount--
        if (sendChannelCount == 0) return teardown()

        val toRemove = FastArrayList<Task>()
        for (task in waitingSends) {
            if (task.customSuspendData == channel) {
                task.reschedule(Cancellable.failed(AlreadyClosedError))
                toRemove.add(task)
            }
        }

        toRemove.forEach {
            waitingSends.removeTask(it)
            waitingData.remove(it)
        }
    }

    internal fun closeReceiveChannel(channel: MemoryReceiveChannel<E>) {
        receiveChannelCount--

        if (receiveChannelCount == 0) return teardown()

        val toRemove = FastArrayList<Task>()
        for (task in waitingReceives) {
            if (task.customSuspendData == channel) {
                task.reschedule(Cancellable.failed(AlreadyClosedError))
                toRemove.add(task)
            }
        }

        toRemove.forEach(waitingReceives::removeTask)
    }

    internal suspend fun send(
        channel: MemorySendChannel<E>,
        item: E
    ): CancellableResult<Unit, AlreadyClosedError> {
        if (closed) return Cancellable.failed(AlreadyClosedError)
        val task = getCurrentTask()

        if (waitingReceives.size > 0) {
            val next = waitingReceives.removeFirst()!!
            next.reschedule(passedInValue = Cancellable.ok(item))
            return task.uncancellableCheckpoint(Unit)
        }

        if (bufferSize > 0 && buffer.size < bufferSize) {
            buffer.addLast(item)
            return task.uncancellableCheckpoint(Unit)
        }

        waitingData[task] = item
        waitingSends.append(task)
        task.customSuspendData = channel
        val res = task.suspendTask() as CancellableResult<Unit, AlreadyClosedError>

        // make sure to clean up after ourselves
        if (res.isCancelled) {
            waitingSends.removeTask(task)
            waitingData.remove(task)
        }

        return res
    }

    private fun receiveWithoutWaiting(): E? {
        if (closed) return null

        if (buffer.size > 0) {
            val item = buffer.removeFirst()

            // make sure to update any waiting send channels
            if (waitingSends.size > 0) {
                val nextTask = waitingSends.removeFirst()!!
                val data = waitingData.remove(nextTask)
                    ?: error("somehow, waitingData[task] was null")
                buffer.addLast(data)
                nextTask.reschedule(Cancellable.empty())
            }

            return item
        }

        if (waitingSends.size == 0) return null

        // wake up sending task now
        val top = waitingSends.removeFirst()!!
        val data = waitingData.remove(top) ?: error("somehow, waitingData[task] was null")
        top.reschedule(Cancellable.empty())
        return data
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun receive(
        channel: MemoryReceiveChannel<E>
    ): CancellableResult<E, AlreadyClosedError> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val task = getCurrentTask()
        if (task.checkIfCancelled().isCancelled) {
            return Cancellable.cancelled()
        }

        val item = receiveWithoutWaiting()
        if (item != null) {
            return task.uncancellableCheckpoint(item)
        }

        waitingReceives.append(task)
        task.customSuspendData = channel

        // sender task directly puts the value into our task
        val result = waitUntilRescheduled()

        if (result.isCancelled) waitingReceives.removeTask(task)
        return result as CancellableResult<E, AlreadyClosedError>
    }
}

/**
 * An implementation of [ReceiveChannel] that uses in-memory transports.
 */
public class MemoryReceiveChannel<E : Any>
internal constructor(private val state: MemoryChannelState<E>) : ReceiveChannel<E>, AsyncIterable<E> {
    override var closed: Boolean = false
        get() {
            return if (state.closed) true
            else field
        }

    /**
     * Gets the [MemoryChannelStatistics] for the underlying channel that this channel is connected
     * to.
     */
    public fun statistics(): MemoryChannelStatistics {
        return state.statistics()
    }

    override suspend fun iterator(): AsyncIterator<E> {
        return object : AsyncIterator<E> {
            override suspend fun hasNext(): Boolean {
                return !closed
            }

            override suspend fun next(): E {
                val res = receive()
                if (!res.isSuccess) throw NoSuchElementException()
                return res.get()!!
            }
        }
    }

    override fun close() {
        state.closeReceiveChannel(this)
    }

    override fun clone(): Either<MemoryReceiveChannel<E>, AlreadyClosedError> {
        if (closed) {
            return Either.err(AlreadyClosedError)
        }

        state.receiveChannelCount++
        return Either.ok(MemoryReceiveChannel<E>(state))
    }

    override suspend fun receive(): CancellableResult<E, AlreadyClosedError> {
        return state.receive(this)
    }
}

/**
 * An implementation of [SendChannel] that uses in-memory transports.
 */
public class MemorySendChannel<E : Any>
internal constructor(private val state: MemoryChannelState<E>) : SendChannel<E> {
    override var closed: Boolean = false
        get() {
            return if (state.closed) true
            else field
        }

    /**
     * Gets the [MemoryChannelStatistics] for the underlying channel that this channel is connected
     * to.
     */
    public fun statistics(): MemoryChannelStatistics {
        return state.statistics()
    }

    override fun close() {
        state.closeSendChannel(this)
        closed = true
    }

    override fun clone(): Either<MemorySendChannel<E>, AlreadyClosedError> {
        if (closed || state.closed) {
            closed = true
            return Either.err(AlreadyClosedError)
        }

        state.sendChannelCount++
        return Either.ok(MemorySendChannel<E>(state))
    }

    override suspend fun send(item: E): CancellableResult<Unit, AlreadyClosedError> {
        return state.send(this, item)
    }
}

/**
 * Opens a pair of ([ReceiveChannel], [SendChannel]) that send and receive objects in-memory.
 *
 * [bufferSize] controls the number of items that can be buffered in this channel before send calls
 * would block.
 *
 * If [bufferSize] is zero, then all send calls will block until there is a receive call, and the
 * item is moved directly from the send to the receive call.
 *
 * [bufferSize] cannot be a negative integer.
 */
public fun <E : Any> openMemoryChannelPair(
    bufferSize: Int = 0
): Pair<MemoryReceiveChannel<E>, MemorySendChannel<E>> {
    assert(bufferSize >= 0) { "why are you passing $bufferSize to this function?" }

    val state = MemoryChannelState<E>(bufferSize)

    state.sendChannelCount++
    state.receiveChannelCount++

    return Pair(MemoryReceiveChannel(state), MemorySendChannel(state))
}
