/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package tf.veriny.wishport.internals.io

import external.liburing.*
import kotlinx.cinterop.*
import platform.extra.*
import platform.posix.*
import platform.posix.EAGAIN
import platform.posix.EBUSY
import platform.posix.EINTR
import platform.posix.ETIME
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.core.InternalWishportError
import tf.veriny.wishport.core.NS_PER_SEC
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode
import tf.veriny.wishport.sync.CapacityLimiter
import tf.veriny.wishport.util.kstrerror

// TODO: consider enabling poll mode by default
// TODO: I was wrong about how the SQ works, so maybe remove the capacity limiter?
//       Right now it avoids the apparent slow kernel buffering though...

private val MISSING_NODROP = """
    io_uring reports that it is missing the IORING_FEAT_NODROP feature. This feature is essential to
    Wishport. You should upgrade your kernel!
""".trimIndent()

/**
 * Provides asynchronous access to the io_uring kernel interface, via liburing.
 */
@Unsafe
@LowLevelApi
public actual class IOManager(
    size: Int,
    private val pollMode: Boolean = false,
) : Closeable {
    public actual companion object {
        public actual fun default(): IOManager {
            return IOManager(2048, false)
        }
    }

    private var closed = false
    private val alloca = Arena()

    // THE io_uring
    private val ring = alloca.alloc<io_uring>()
    // blatantly ignoring my own safety rules here
    private val limiter = CapacityLimiter(size, Unit)

    // used for associating tasks and cqes
    private var counter = 0UL
    // todo: replace this with a generificified longmap, or hell perhaps even another ring
    // buffer.
    private val tasks = mutableMapOf<ULong, SleepingTask>()

    init {
        memScoped {
            val params = alloc<io_uring_params>()
            memset(params.ptr, 0, sizeOf<io_uring_params>().convert())

            // SUBMIT_ALL moves errors down the chain to stupid users
            // COOP_TASKRUN is just faster
            // TASKRUN_FLAG is also faster
            // CQSIZE lets us customise the completion queue size
            var flags = flags(
                IORING_SETUP_SUBMIT_ALL,
                IORING_SETUP_COOP_TASKRUN,
                IORING_SETUP_TASKRUN_FLAG,
                IORING_SETUP_CQSIZE,
                IORING_SETUP_CLAMP,
            )

            if (pollMode) flags = flags.or(IORING_SETUP_SQPOLL)

            // completion queue needs N entries for possible results and N entries for possible
            // cancellations.
            // submission queue is set to some high number to allow polling mode to work
            // TODO: determine if this is the right number anyway
            params.cq_entries = (size * 2).convert()
            params.flags = flags

            val res = io_uring_queue_init_params(
                256, ring.ptr, params.ptr
            )

            if (res < 0) {
                val result = kstrerror(posix_errno())
                throw InternalWishportError("io_uring_queue_init failed: $result")
            }

            // check features
            val features = params.features
            if (features.and(IORING_FEAT_NODROP) == 0U) {
                close()
                throw InternalWishportError(MISSING_NODROP)
            }
        }
    }

    private fun handleCqe(cqe: CPointerVar<io_uring_cqe>) {
        val unwrapped = cqe.pointed!!
        val task = tasks[unwrapped.user_data]
        if (task == null) {
            // TODO: don't println
            println("WARN: Incoming CQE #${unwrapped.user_data} has no associated task")
            return
        }

        // successful cancellation
        if (unwrapped.res == -EINTR) {
            task.completed = true
            task.wakeupData = Cancellable.cancelled()
            return
        }

        // gross!
        val data: CancellableResourceResult<IOResult> = when (task.why) {
            // ignore cancellation requests
            SleepingWhy.CANCEL -> {
                task.completed = true
                task.wakeupData = Cancellable.cancelled()
                return
            }

            // impl note, these are the same (openat2) on linux
            SleepingWhy.OPEN_DIRECTORY, SleepingWhy.OPEN_FILE -> {
                val fd = unwrapped.res
                if (fd < 0) Cancellable.failed(abs(fd).toSysError())
                else Cancellable.ok(Fd(fd))
            }

            SleepingWhy.READ_WRITE -> {
                val readCount = unwrapped.res
                if (readCount < 0) Cancellable.failed(abs(readCount).toSysError())
                else Cancellable.ok(ByteCountResult(readCount))
            }
        }

        task.completed = true
        task.wakeupData = data
        task.task.reschedule()
    }

    // e.g. wait with a timeout would use io_uring_cqe_wait_nr
    // this is designed to peek all remaining items off to allow more than one task to be rescheduled
    // later.
    /**
     * Waits for I/O. This will first use [block] to wait for the first completion, then peek off
     * any remaining entries from the queue after that.
     */
    private inline fun pollIO(block: (CPointerVar<io_uring_cqe>) -> Int): Unit = memScoped {
        // may return -EAGAIN, which means no completions, which means we drop it
        // i think io_uring_wait_cqe_nr might do that if there's nothing in the queue but im
        // not really sure.
        val cqe = allocPointerTo<io_uring_cqe>()
        run {
            val res = block(cqe)
            if (res == -EAGAIN) return
            if (res == -ETIME) return
            else if (res < 0) {
                val err = kstrerror(abs(res))
                throw InternalWishportError("io_uring function returned $err")
            }
        }

        while (cqe.pointed != null) {
            handleCqe(cqe)
            io_uring_cqe_seen(ring.ptr, cqe.value)
            cqe.pointed = null

            val res = io_uring_peek_cqe(ring.ptr, cqe.ptr)
            if (res == -EAGAIN) break
        }
    }

    /**
     * Peeks off all pending I/O events, and wakes up tasks that would be waiting.
     */
    public actual fun pollIO() {
        pollIO { io_uring_peek_cqe(ring.ptr, it.ptr) }
    }

    /**
     * Waits for I/O forever.
     */
    public actual fun waitForIO() {
        pollIO { io_uring_wait_cqe(ring.ptr, it.ptr) }
    }

    /**
     * Waits for I/O for [timeout] nanoseconds.
     */
    public actual fun waitForIOUntil(timeout: Long): Unit = memScoped {
        val ts = alloc<__kernel_timespec>()
        ts.tv_sec = (timeout / NS_PER_SEC)
        ts.tv_nsec = (timeout.rem(1_000_000_000))

        pollIO {
            io_uring_wait_cqe_timeout(ring.ptr, it.ptr, ts.ptr)
        }
    }

    // todo: see if we can outrun the poller at some point.
    @Suppress("FoldInitializerAndIfToElvis")
    private inline fun getsqe(): CPointer<io_uring_sqe> {
        // the submission queue should be, by far, big enough for all SQEs.
        // if this isn't the case, then we have a BIG problem
        val res = io_uring_get_sqe(ring.ptr)
        if (res == null) {
            if (pollMode) {
                throw InternalWishportError(
                    "You outran the kernel's poll thread somehow! " +
                        "Please report this bug for a free cat picture."
                )
            } else {
                throw InternalWishportError("sqe is null, this shouldn't happen ever")
            }
        }

        return res
    }

    private fun submit() {
        // XXX: in poll mode, this is essentially a null-op.
        //      thanks liburing! genuinely great feature, makes my life 10000x easier
        //      as i dont have to care about if i actually should enter or not, just pawn it off
        //      to the end user.
        val submitResult = io_uring_submit(ring.ptr)

        // generally, i believe this means very bad things!
        // io_uring_enter(2)'s man page gives a handful of errors but they're generally
        // related to using io_uring_enter directly. liburing should have set everything up
        // so that it's all fine.
        if (submitResult < 0) {
            if (submitResult == -EBUSY) {
                // this should NEVER happen. the capacity limiter should stop this.
                // i'm adding a to-do in case it turns out I *do* need to care about this,
                // but otherwise it's unhandled.
                TODO("handle io_uring_submit -EBUSY")
            } else {
                val result = kstrerror(posix_errno())
                throw InternalWishportError("io_uring_submit failed: $result")
            }
        }
    }

    /**
     * Waits for the recently submitted action to be completed.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : IOResult> submitAndWait(
        task: Task,
        id: ULong,
        why: SleepingWhy
    ): CancellableResourceResult<T> {
        // add first, idk why, just vibes
        val sleepy = SleepingTask(task, id, why)
        tasks[id] = sleepy

        submit()

        // scary use of low-level primitives here
        // safety: this is being ran INSIDE the checkCancelled call, so it should be alright.
        val result = waitUntilRescheduled()

        // either we got rescheduled as normal, or the cqe got finished before we got a chance
        // to actually process cancellation
        // in the latter case, we just pretend that actually we never got cancelled. not our
        // problem!
        if (!result.isCancelled || sleepy.completed) {
            return sleepy.wakeupData as CancellableResourceResult<T>
        }

        // sad path, we have to send a cancel request
        // io_uring cancellation is.... complicated
        val cancellationId = counter++
        val sleep2 = SleepingTask(task, cancellationId, why)
        tasks[cancellationId] = sleep2

        val sqe = getsqe()
        io_uring_prep_cancel64(sqe, id, 0)
        io_uring_sqe_set_data64(sqe, cancellationId)
        submit()

        // we can't use the result here as we are DEFINITELY cancelled.
        // the completion poller will wake us up, and if the cancellation request didn't go through,
        // we pretend as if we didn't get cancelled.
        // otherwise, it'll have put a finished on there and we can return that.
        // the second one we can ignore, as cancellations are handled specifically by the io loop
        // and silently dropped.

        waitUntilRescheduled()

        assert(sleepy.completed)
        return sleepy.wakeupData as CancellableResourceResult<T>
    }

    // some notes on stability
    // until kernel 5.5, all submission queue entries had to have their data remain stable in memory
    // whilst processing
    // post-5.5, we only have to be stable until the enter() call. but because we suspend, the data
    // stays within the memScope no matter what, so we'll work on both >=5.5 and <5.5.

    // note: these still need the casts as the capacitylimiter converts the result to just Fail
    //       as it could return AlreadyAcquired.
    //       but we know that's not the case, so we just cast it and override it.

    /**
     * Opens a new handle to a directory.
     */
    @Suppress("UNCHECKED_CAST")
    public actual suspend fun openFilesystemDirectory(
        dirHandle: DirectoryHandle?,
        path: ByteString
    ): CancellableResourceResult<DirectoryHandle> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                limiter.unwrapAndRun {
                    val sqe = getsqe()
                    val how = alloc<open_how>()
                    // see openat(2), this is mandatory
                    memset(how.ptr, 0, sizeOf<open_how>().convert())
                    how.flags = flags(O_RDWR, _O_PATH, O_CLOEXEC, O_DIRECTORY).convert()

                    val dirfd = dirHandle?.actualFd ?: _AT_FDCWD

                    // we live in a world where exceptions are fatal, so this is ok.
                    val pin = path.pinnedTerminated()
                    defer { pin.unpin() }
                    val cPath = pin.addressOf(0)

                    io_uring_prep_openat2(sqe, dirfd, cPath, how.ptr)
                    io_uring_sqe_set_data64(sqe, counter++)
                    submitAndWait<DirectoryHandle>(
                        task, sqe.pointed.user_data, SleepingWhy.OPEN_DIRECTORY
                    )
                }
            } as CancellableResourceResult<DirectoryHandle>
    }

    @Suppress("UNCHECKED_CAST")
    public actual suspend fun openFilesystemFile(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>
    ): CancellableResourceResult<RawFileHandle> = memScoped {
        var openFlags = O_CLOEXEC

        for (flag in flags) {
            when (flag) {
                FileOpenFlags.APPEND -> {
                    openFlags = openFlags.or(O_APPEND)
                }
                FileOpenFlags.CREATE_IF_NOT_EXISTS -> {
                    openFlags = openFlags.or(O_CREAT)
                }
                FileOpenFlags.MUST_CREATE -> {
                    openFlags = openFlags.or(O_CREAT).or(O_EXCL)
                }
                FileOpenFlags.DIRECT -> {
                    openFlags = openFlags.or(_O_DIRECT).or(O_SYNC)
                }
                FileOpenFlags.NO_ACCESS_TIME -> {
                    openFlags = openFlags.or(_O_NOATIME)
                }
                FileOpenFlags.NO_FOLLOW -> {
                    // TODO: allow customising this properly with openat2() semantics
                    openFlags = openFlags.or(O_NOFOLLOW)
                }
                FileOpenFlags.TEMPORARY_FILE -> {
                    openFlags = openFlags.or(_O_TMPFILE)
                }
                FileOpenFlags.TRUNCATE -> {
                    openFlags = openFlags.or(O_TRUNC)
                }
            }
        }

        val openMode = when (mode) {
            FileOpenMode.READ_ONLY -> O_RDONLY
            FileOpenMode.READ_WRITE -> O_RDWR
            FileOpenMode.WRITE_ONLY -> O_WRONLY
        }
        openFlags = openFlags.or(openMode)

        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                limiter.unwrapAndRun {
                    val sqe = getsqe()
                    val how = alloc<open_how>()
                    // see openat(2), this is mandatory
                    memset(how.ptr, 0, sizeOf<open_how>().convert())
                    how.flags = openFlags.convert()

                    val dirfd = dirHandle?.actualFd ?: _AT_FDCWD
                    val pin = path.pinnedTerminated()
                    defer { pin.unpin() }
                    val cPath = pin.addressOf(0)

                    io_uring_prep_openat2(sqe, dirfd, cPath, how.ptr)
                    io_uring_sqe_set_data64(sqe, counter++)
                    submitAndWait<RawFileHandle>(
                        task, sqe.pointed.user_data, SleepingWhy.OPEN_FILE
                    )
                }
            } as CancellableResourceResult<RawFileHandle>
    }

    private fun checkBuffers(
        buf: ByteArray,
        size: UInt,
        offset: Int
    ): CancellableResult<Unit, Fail> {
        if (size < 0U || size > buf.size.toUInt()) {
            return Cancellable.failed(TooSmall(size))
        }

        if (offset < 0 || offset >= buf.size) {
            return Cancellable.failed(IndexOutOfRange(offset.toUInt()))
        }

        val requiredEndPoint = size + offset.toUInt()
        if (buf.size.toUInt() < requiredEndPoint) {
            return Cancellable.failed(TooSmall(requiredEndPoint))
        }

        return Cancellable.empty()
    }

    @Suppress("UNCHECKED_CAST")
    public actual suspend fun read(
        handle: ReadableHandle,
        out: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int,
    ): CancellableResult<ByteCountResult, Fail> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen { checkBuffers(out, size, bufferOffset) }
            .andThen {
                limiter.unwrapAndRun {
                    val sqe = getsqe()
                    val buf = out.pin()

                    io_uring_prep_read(
                        sqe, handle.actualFd, buf.addressOf(bufferOffset), size, fileOffset
                    )
                    io_uring_sqe_set_data64(sqe, counter++)
                    submitAndWait<ByteCountResult>(
                        task,
                        sqe.pointed.user_data,
                        SleepingWhy.OPEN_FILE
                    )
                }
            }
    }

    override fun close() {
        if (closed) return

        io_uring_queue_exit(ring.ptr)
        alloca.clear()
        closed = true
    }
}
