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
import platform.linux.*
import platform.posix.*
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.core.*
import tf.veriny.wishport.internals.Task
import tf.veriny.wishport.internals.checkIfCancelled
import tf.veriny.wishport.internals.uncancellableCheckpoint
import tf.veriny.wishport.io.*
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.io.net.Inet4SocketAddress
import tf.veriny.wishport.io.net.Inet6SocketAddress
import tf.veriny.wishport.io.net.SocketAddress
import tf.veriny.wishport.util.*
import kotlin.math.min

// TODO: consider enabling poll mode by default

private val EMPTY_PATH = byteArrayOf(0)

// TODO: Don't hardcode this.
private const val STATX_BASIC_STATS = 2047U

private fun statx_timestamp.toNs(): ULong {
    return (tv_sec.toULong() * NS_PER_SEC.toULong()) + tv_nsec.toULong()
}

private fun CPointer<io_uring_sqe>.setuserid(ref: StableRef<*>) {
    val ptr = ref.asCPointer()
    io_uring_sqe_set_data(this, ptr)
    // io_uring_sqe_set_data64(this, ptr)
}

private fun CPointer<io_uring_sqe>.link() {
    pointed.flags = pointed.flags.or(IOSQE_IO_LINK.toUByte())
}

private fun CPointerVar<io_uring_cqe>.getTask(): SleepingTask {
    val ptr = io_uring_cqe_get_data(this.value)!!
    val ref = ptr.asStableRef<SleepingTask>()
    return ref.get()
}

/**
 * Provides asynchronous access to the io_uring kernel interface, via liburing.
 */
@LowLevelApi
public actual class IOManager(
    size: Int,
    private val pollMode: Boolean = false,
) : Closeable {
    public actual companion object {
        // even if you handle 1 million events per second, the SQE counter wouldn't roll over
        // to this value for 580_000 years.
        private const val EFD_SQE = 0xFFFF_FFFF_FFFF_0000UL

        public actual fun default(): IOManager {
            return IOManager(2048, false)
        }

        public actual fun withSize(size: Int): IOManager {
            return IOManager(size, false)
        }
    }

    public override var closed: Boolean = false
        private set

    private val alloca = Arena()

    // THE io_uring
    private val ring = alloca.alloc<io_uring>()

    // used for thread-side wake ups
    @Suppress("RemoveRedundantQualifierName")
    private val efd = platform.extra.eventfd(0, EFD_CLOEXEC)

    init {
        memScoped {
            val params = alloc<io_uring_params>()
            memset(params.ptr, 0, sizeOf<io_uring_params>().convert())
            var flags = flags(IORING_SETUP_CQSIZE, IORING_SETUP_CLAMP)

            val uname = try {
                getKernelVersion()
            } catch (e: ProcNotFoundError) {
                KernelVersion(5, 15, 0)
            }
            // check for at least kernel 5.15, which is needed for mkdirat/symlinkat/linkat.
            if (uname.major < 5 || (uname.major < 6 && uname.minor < 15)) {
                throw InternalWishportError("Wishport requires at least Linux kernel 5.15")
            }

            when {
                uname.major >= 6 -> {
                    // we're single threaded wrt the io_uring so this is good and fast
                    flags = flags.or(IORING_SETUP_SINGLE_ISSUER)
                }
                uname.major >= 6 || uname.minor >= 19 -> {
                    flags = flags.or(IORING_SETUP_COOP_TASKRUN)
                    flags = flags.or(IORING_SETUP_TASKRUN_FLAG)
                }
                uname.major >= 6 || uname.minor >= 18 -> {
                    flags = flags.or(IORING_SETUP_SUBMIT_ALL)
                }
            }

            if (pollMode) flags = flags.or(IORING_SETUP_SQPOLL)

            // completion queue needs N entries for possible results and N entries for possible
            // cancellations.
            // submission queue is set to some high number to allow polling mode to work
            // TODO: determine if this is the right number anyway
            params.cq_entries = (size * 2).convert()
            params.flags = flags

            val res = io_uring_queue_init_params(
                min(size, 256).toUInt(), ring.ptr, params.ptr
            )

            if (res < 0) {
                val result = kstrerror(abs(res))
                throw InternalWishportError("io_uring_queue_init failed: $result")
            }

            setupEventFdPoller()
        }
    }

    private fun setupEventFdPoller() {
        val sqe = getsqe()
        io_uring_prep_poll_multishot(sqe, efd, EPOLLIN.toUInt().or(EPOLLET))
        io_uring_sqe_set_data64(sqe, EFD_SQE)
        submit()
    }

    private fun handleCqe(cqe: CPointerVar<io_uring_cqe>) {
        val unwrapped = cqe.pointed!!

        // ignore the eventfd writes, this is just to wake us up.
        if (unwrapped.user_data == EFD_SQE) { return }

        val task = cqe.getTask()

        val result = unwrapped.res

        // potential error result
        if (result < 0) {
            // for linked SQEs, we only store the result if there's been no other error result
            // as the others will all have EINTR/ECANCELED.
            if (task.wakeupData == null) {
                // cancelled normally
                if (result == -EINTR || result == -ECANCELED) {
                    task.wakeupData = Cancellable.cancelled()
                } else {
                    task.wakeupData = Cancellable.failed(abs(unwrapped.res).toSysError())
                }
            } else {
                // error result from a linked task, *should* be a cancelled if it follows the
                // other SQE values.
                assert(result == -EINTR || result == -ECANCELED) {
                    "something went wrong inside the i/o manager, please report"
                }
            }

            task.sqeCount--
            if (task.sqeCount == 0) {
                task.completed = true
                task.task.reschedule(task.wakeupData!!)
            }

            return
        }

        // gross!
        // TODO: replace this with enum methods maybe
        val data = when (task.why) {
            // ignore SUCCESSFUL cancellation requests
            SleepingWhy.CANCEL -> {
                task.completed = true
                task.wakeupData = Cancellable.cancelled()
                return
            }

            // impl note, these are the same (openat2) on linux
            SleepingWhy.OPEN_DIRECTORY, SleepingWhy.OPEN_FILE, SleepingWhy.ACCEPT -> {
                val fd = unwrapped.res
                Cancellable.ok(Fd.get(fd))
            }

            SleepingWhy.READ_WRITE -> {
                Cancellable.ok(ByteCountResult(result.toUInt()))
            }

            SleepingWhy.POLL_ADD -> {
                Cancellable.ok(PollResult(intoFlags(result)))
            }

            SleepingWhy.FSYNC, SleepingWhy.CLOSE,
            SleepingWhy.POLL_UPDATE, SleepingWhy.MKDIR,
            SleepingWhy.UNLINK, SleepingWhy.SHUTDOWN,
            SleepingWhy.STATX, SleepingWhy.CONNECT,
            SleepingWhy.RENAME, SleepingWhy.SYMLINK,
            SleepingWhy.LINK, -> {
                Empty.RESULT
            }
        }

        task.sqeCount--
        if (task.sqeCount == 0) {
            task.completed = true
            task.wakeupData = data
            task.task.reschedule(task.wakeupData!!)
        }
    }

    // e.g. wait with a timeout would use io_uring_cqe_wait_nr
    // this is designed to peek all remaining items off to allow more than one task to be rescheduled
    // later.
    /**
     * Waits for I/O. This will first use [block] to wait for the first completion, then peek off
     * any remaining entries from the queue after that.
     */
    private inline fun pollIO(block: (CPointerVar<io_uring_cqe>) -> Int): Int = memScoped {
        // may return -EAGAIN, which means no completions, which means we drop it
        // i think io_uring_wait_cqe_nr might do that if there's nothing in the queue but im
        // not really sure.
        var count = 0

        val cqe = allocPointerTo<io_uring_cqe>()
        run {
            val res = block(cqe)
            if (res == -EAGAIN) return 0
            if (res == -ETIME) return 0
            else if (res < 0) {
                val err = kstrerror(abs(res))
                throw InternalWishportError("io_uring function returned $err")
            }
        }

        while (cqe.pointed != null) {
            count++
            handleCqe(cqe)
            io_uring_cqe_seen(ring.ptr, cqe.value)
            cqe.pointed = null

            val res = io_uring_peek_cqe(ring.ptr, cqe.ptr)
            if (res == -EAGAIN) break
        }

        return count
    }

    /**
     * Peeks off all pending I/O events, and wakes up tasks that would be waiting.
     */
    public actual fun pollIO(): Int = pollIO { io_uring_peek_cqe(ring.ptr, it.ptr) }

    /**
     * Waits for I/O forever.
     */
    public actual fun waitForIO(): Int = pollIO { io_uring_wait_cqe(ring.ptr, it.ptr) }

    /**
     * Waits for I/O for [timeout] nanoseconds.
     */
    public actual fun waitForIOUntil(timeout: Long): Int = memScoped {
        val ts = alloc<__kernel_timespec>()
        ts.tv_sec = (timeout / NS_PER_SEC)
        ts.tv_nsec = (timeout.rem(1_000_000_000))

        pollIO {
            io_uring_wait_cqe_timeout(ring.ptr, it.ptr, ts.ptr)
        }
    }

    public actual fun forceWakeUp() {
        eventfd_write(efd, 1)
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

    private fun submit(): Int {
        // XXX: in poll mode, this is essentially a null-op.
        //      thanks liburing! genuinely great feature, makes my life 10000x easier
        //      as i dont have to care about if i actually should enter or not, just pawn it off
        //      to the end user.
        var submitResult: Int

        // run loop submission in a loop so that we can immediately reap and reschedule all waiting
        // for I/O tasks if the completion queue is full.
        while (true) {
            submitResult = io_uring_submit(ring.ptr)

            if (submitResult >= 0) break
            else if (submitResult == -EBUSY) {
                // EBUSY  If the IORING_FEAT_NODROP feature flag is set, then EBUSY will be returned
                // if there were overflow entries, IORING_ENTER_GETEVENTS flag is
                // set and not all of the overflow entries were able to be flushed to the CQ ring.
                // not enough data
                // we just directly reap here and reschedule.
                if (pollIO() == 0) {
                    // this is very bad and probably an error!
                    throw InternalWishportError("io_uring_submit returrned -EBUSY, but there are no tasks to reap!")
                }
            } else {
                val result = kstrerror(abs(submitResult))
                throw InternalWishportError("io_uring_submit failed: $result")
            }
        }

        return submitResult
    }

    /**
     * Waits for the recently submitted action to be completed.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : IOResult> submitAndWait(
        task: Task,
        sleepy: SleepingTask,
        ref: StableRef<SleepingTask>
    ): CancellableResourceResult<T> {
        assert(pollMode || io_uring_sq_ready(ring.ptr) > 0U) {
            "submitAndWait was called without anything to submit, this is probably a Wishport bug! " +
                "please report!"
        }

        submit()

        // scary use of low-level primitives here
        // safety: this is being ran INSIDE the checkCancelled call, so it should be alright.
        val result = waitUntilRescheduled()

        // either we got rescheduled as normal, or the cqe got finished before we got a chance
        // to actually process cancellation
        // in the latter case, we just pretend that actually we never got cancelled. not our
        // problem!
        if (!result.isCancelled || sleepy.completed) {
            ref.dispose()
            return sleepy.wakeupData!! as CancellableResourceResult<T>
        }

        // sad path, we have to send a cancel request
        // io_uring cancellation is.... complicated
        val firstTaskRefId = ref.asCPointer().toLong().toULong()
        val sleep2 = SleepingTask(task, SleepingWhy.CANCEL)
        val newTaskRef = StableRef.create(sleep2)

        // for poll events, we can't use prep_cancel (... as far as I know, anyway)
        // so we have to use poll_remove. it's the same thing, according to the man pages.
        val sqe = getsqe()
        if (sleepy.why == SleepingWhy.POLL_ADD) {
            io_uring_prep_poll_remove(sqe, firstTaskRefId)
        } else {
            io_uring_prep_cancel64(sqe, firstTaskRefId, 0)
        }

        sqe.setuserid(newTaskRef)
        submit()

        // we can't use the result here as we are DEFINITELY cancelled.
        // the completion poller will wake us up, and if the cancellation request didn't go through,
        // we pretend as if we didn't get cancelled.
        // otherwise, it'll have put a finished on there and we can return that.
        // the second one we can ignore, as cancellations are handled specifically by the io loop
        // and silently dropped.

        waitUntilRescheduled()

        ref.dispose()
        newTaskRef.dispose()

        assert(sleepy.completed) { "task should be finished by now!!!" }
        return sleepy.wakeupData as CancellableResourceResult<T>
    }

    private suspend inline fun <T : IOResult> submitAndWait(
        task: Task,
        why: SleepingWhy,
        sqeCount: Int = 1,
        block: (StableRef<SleepingTask>) -> Unit
    ): CancellableResourceResult<T> {
        val sleepy = SleepingTask(task, why)
        sleepy.sqeCount = sqeCount
        val ref = StableRef.create(sleepy)
        ref.asCPointer()

        block(ref)

        // the ref is disposed by submitAndWait (real)
        return submitAndWait(task, sleepy, ref)
    }
    private suspend fun close(fd: Int, task: Task): CancellableResourceResult<Empty> {
        val sqe = getsqe()
        io_uring_prep_close(sqe, fd)

        return submitAndWait(task, SleepingWhy.CLOSE) { sqe.setuserid(it) }
    }

    public actual suspend fun closeHandle(handle: IOHandle): CancellableResourceResult<Empty> {
        val task = getCurrentTask()
        assert(!task.checkIfCancelled().isCancelled) {
            "closeHandle should never be called from a cancelled context!"
        }

        return close(handle.actualFd, task)
    }

    public actual suspend fun closeMany(vararg handles: IOHandle): CancellableResourceResult<Empty> {
        if (handles.isEmpty()) {
            // bruh
            return Empty.RESULT
        }

        val task = getCurrentTask()
        assert(!task.checkIfCancelled().isCancelled) {
            "closeHandle should never be called from a cancelled context!"
        }

        val sleepy = SleepingTask(task, SleepingWhy.CLOSE)
        sleepy.sqeCount = 0
        val ref = StableRef.create(sleepy)

        for (handle in handles) {
            // bypass a lot of our safety logic...
            // we repeatedly add close requests onto the loop until the queue is full, then
            // submit. this avoids repeated io_uring_enter() calls.

            var sqe = io_uring_get_sqe(ring.ptr)
            if (sqe == null) {
                submit()

                // safe call now, as the queue will be empty
                sqe = getsqe()
            }

            io_uring_prep_close(sqe, handle.actualFd)
            sqe.setuserid(ref)
            sleepy.sqeCount++
        }

        // no need to dispose ref, submitAndWait does it for us.
        return submitAndWait(task, sleepy, ref)
    }

    public actual suspend fun shutdown(
        handle: IOHandle,
        how: ShutdownHow
    ): CancellableResourceResult<Empty> {
        val task = getCurrentTask()

        val why = when (how) {
            ShutdownHow.READ -> SHUT_RD
            ShutdownHow.WRITE -> SHUT_WR
            ShutdownHow.BOTH -> SHUT_RDWR
        }

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()

                io_uring_prep_shutdown(sqe, handle.actualFd, why)

                submitAndWait(task, SleepingWhy.SHUTDOWN) { sqe.setuserid(it) }
            }
    }

    public actual suspend fun closeSocket(socket: SocketHandle): CancellableResourceResult<Empty> {
        val task = getCurrentTask()
        assert(!task.checkIfCancelled().isCancelled) {
            "closeHandle should never be called from a cancelled context!"
        }

        // try 1: submit a linked shutdown->close request
        // this should work for most sockets that don't explicitly send_eof().

        val shut = getsqe()
        io_uring_prep_shutdown(shut, socket.actualFd, SHUT_RDWR)
        shut.link()

        val close = getsqe()
        io_uring_prep_close(close, socket.actualFd)

        val result = submitAndWait<Empty>(
            task, SleepingWhy.CLOSE, sqeCount = 2
        ) { shut.setuserid(it); close.setuserid(it) }
        if (!result.isFailure || result.getFailure()!! != TransportEndpointIsNotConnected) {
            return result
        }

        // try 2: just a close().
        val sqe = getsqe()
        io_uring_prep_close(sqe, socket.actualFd)

        return submitAndWait(task, SleepingWhy.CLOSE) { sqe.setuserid(it) }
    }

    /**
     * Opens a new handle to a directory.
     */
    @Unsafe
    public actual suspend fun openFilesystemDirectory(
        dirHandle: DirectoryHandle?,
        path: ByteString
    ): CancellableResourceResult<DirectoryHandle> {
        return openFilesystemFile(
            dirHandle, path, FileOpenType.READ_WRITE,
            setOf(FileOpenFlags.PATH, FileOpenFlags.DIRECTORY),
            setOf()
        )
    }

    @Unsafe
    public actual suspend fun openFilesystemFile(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        type: FileOpenType,
        flags: Set<FileOpenFlags>,
        filePermissions: Set<FilePermissions>,
    ): CancellableResourceResult<RawFileHandle> = memScoped {
        var openFlags = O_CLOEXEC

        // cool fucking fact!
        // O_CREAT is not compatible with O_TMPFILE
        // hijack the flags because this is NOT documented

        for (flag in flags) {
            when (flag) {
                FileOpenFlags.APPEND -> {
                    openFlags = openFlags.or(O_APPEND)
                }
                FileOpenFlags.CREATE_IF_NOT_EXISTS -> {
                    if (FileOpenFlags.TEMPORARY_FILE !in flags) {
                        openFlags = openFlags.or(O_CREAT)
                    }
                }
                FileOpenFlags.MUST_CREATE -> {
                    openFlags = if (FileOpenFlags.TEMPORARY_FILE in flags) {
                        openFlags.or(O_EXCL)
                    } else {
                        openFlags.or(O_CREAT).or(O_EXCL)
                    }
                }
                FileOpenFlags.DIRECT -> {
                    openFlags = openFlags.or(_O_DIRECT).or(O_SYNC)
                }
                FileOpenFlags.DIRECTORY -> {
                    openFlags = openFlags.or(O_DIRECTORY)
                }
                FileOpenFlags.PATH -> {
                    openFlags = openFlags.or(_O_PATH)
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

        val openMode = when (type) {
            FileOpenType.READ_ONLY -> O_RDONLY
            FileOpenType.READ_WRITE -> O_RDWR
            FileOpenType.WRITE_ONLY -> O_WRONLY
        }
        openFlags = openFlags.or(openMode)

        val mode = if (filePermissions.isEmpty()) {
            0U
        } else {
            var perms = (0U).toUShort()
            for (i in filePermissions) {
                perms = perms.or(i.posixNumber)
            }
            perms
        }

        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                // XXX: For some reason openat2() fails where openat() doesn't.

                // val how = alloc<open_how>()
                //  see openat(2), this is mandatory
                // memset(how.ptr, 0, sizeOf<open_how>().convert())
                // how.flags = openFlags.convert()

                val dirfd = dirHandle?.actualFd ?: _AT_FDCWD
                val pin = path.pinnedTerminated()
                defer { pin.unpin() }
                val cPath = pin.addressOf(0)

                // io_uring_prep_openat2(sqe, dirfd, cPath, how.ptr)
                io_uring_prep_openat(sqe, dirfd, cPath, openFlags, mode.toUInt())

                submitAndWait(
                    task, SleepingWhy.OPEN_FILE
                ) { sqe.setuserid(it) }
            }
    }

    // Adding a new socket type:
    // 1) Make a new SocketAddress subclass that encapsulates the data for the socket connection.
    // 2) Add a new branch to the when call in ``doSocketAddress`` that handles the new socket type.
    // This is perhaps not very OO-style code, but I simply do not care. It avoids having to do stupid
    // expect/actual shenanigans on every socket address subclass.

    @Unsafe
    private fun doSocketAddress(
        alloc: NativePlacement,
        handle: IOHandle,
        address: SocketAddress,
        sqe: CPointer<io_uring_sqe>?
    ): Int = with(alloc) {
        when (address) {
            is Inet4SocketAddress -> {
                val addr = alloc<sockaddr_in>()
                memset(addr.ptr, 0, sizeOf<sockaddr_in>().toULong())
                addr.sin_family = AF_INET.toUShort()
                addr.sin_addr.s_addr = htonl(address.address.toUInt())
                addr.sin_port = htons(address.port)

                if (sqe != null) {
                    io_uring_prep_connect(
                        sqe,
                        handle.actualFd,
                        addr.ptr.reinterpret(),
                        sizeOf<sockaddr_in>().convert()
                    )
                } else {
                    return bind(handle.actualFd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
                }
            }
            is Inet6SocketAddress -> {
                val addr = alloc<wp_sockaddr_in6>()
                memset(addr.ptr, 0, sizeOf<wp_sockaddr_in6>().toULong())

                addr.sin6_family = AF_INET6.toUShort()
                address.address.representation.unwrap().usePinned {
                    memcpy(addr.sin6_addr.addr, it.addressOf(0), 16)
                }

                addr.sin6_port = htons(address.port)

                if (sqe != null) {
                    io_uring_prep_connect(
                        sqe,
                        handle.actualFd,
                        addr.ptr.reinterpret(),
                        sizeOf<sockaddr_in6>().toUInt()
                    )
                } else {
                    return bind(handle.actualFd, addr.ptr.reinterpret(), sizeOf<sockaddr_in6>().toUInt())
                }
            }
        }

        return 0
    }

    // not really asynchronous (yet), but we have itt here for future direct sockets
    // and also so that the stupid sockaddr type punning can be unified here
    @OptIn(Unsafe::class)
    public actual suspend fun bind(
        sock: SocketHandle,
        address: SocketAddress
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val res = doSocketAddress(this, sock, address, null)
                if (res < 0) posix_errno().toSysResult().notCancelled()
                else uncancellableCheckpoint(Empty)
            }
    }

    /**
     * Accepts a single incoming connection on a socket.
     */
    @Unsafe
    public actual suspend fun accept(sock: SocketHandle): CancellableResourceResult<SocketHandle> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                io_uring_prep_accept(sqe, sock.actualFd, null, null, SOCK_CLOEXEC)

                submitAndWait(task, SleepingWhy.ACCEPT) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun connect(
        sock: SocketHandle,
        address: SocketAddress
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                // ignore return value, it's always 0
                doSocketAddress(this, sock, address, sqe)

                submitAndWait(task, SleepingWhy.CONNECT) { sqe.setuserid(it) }
            }
    }

    @Unsafe
    public actual suspend fun read(
        handle: IOHandle,
        ptr: COpaquePointer,
        size: UInt,
        fileOffset: ULong,
    ): CancellableResourceResult<ByteCountResult> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                io_uring_prep_read(sqe, handle.actualFd, ptr, size, fileOffset)
                submitAndWait(task, SleepingWhy.READ_WRITE) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun read(
        handle: IOHandle,
        out: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int,
    ): CancellableResult<ByteCountResult, Fail> {
        return out.checkBuffers(size, bufferOffset).andThen {
            it.usePinned { pinned ->
                read(handle, pinned.addressOf(bufferOffset), size, fileOffset)
            }
        }
    }

    @Unsafe
    public actual suspend fun write(
        handle: IOHandle,
        ptr: COpaquePointer,
        size: UInt,
        fileOffset: ULong,
    ): CancellableResourceResult<ByteCountResult> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                io_uring_prep_write(sqe, handle.actualFd, ptr, size, fileOffset)
                submitAndWait(task, SleepingWhy.READ_WRITE) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun write(
        handle: IOHandle,
        input: ByteArray,
        size: UInt,
        fileOffset: ULong,
        bufferOffset: Int
    ): CancellableResult<ByteCountResult, Fail> = memScoped {
        return input.checkBuffers(size, bufferOffset).andThen {
            it.usePinned { pinned ->
                write(handle, pinned.addressOf(bufferOffset), size, fileOffset)
            }
        }
    }

    public actual suspend fun recv(
        handle: IOHandle,
        out: ByteArray,
        size: UInt,
        bufferOffset: Int,
        flags: Int
    ): CancellableResult<ByteCountResult, Fail> = memScoped {
        val task = getCurrentTask()

        val recvFlags = flags.or(MSG_NOSIGNAL)

        return task.checkIfCancelled()
            .andThen { out.checkBuffers(size, bufferOffset).notCancelled() }
            .andThen {
                val sqe = getsqe()
                val pinned = out.pin()

                defer { pinned.unpin() }

                io_uring_prep_recv(sqe, handle.actualFd, pinned.addressOf(bufferOffset), size.toULong(), recvFlags)

                submitAndWait(task, SleepingWhy.READ_WRITE) { sqe.setuserid(it) }
            }
    }

    public actual suspend fun send(
        handle: IOHandle,
        input: ByteArray,
        size: UInt,
        bufferOffset: Int,
        flags: Int
    ): CancellableResult<ByteCountResult, Fail> = memScoped {
        val task = getCurrentTask()

        val sendFlags = flags.or(MSG_NOSIGNAL)

        return task.checkIfCancelled()
            .andThen { input.checkBuffers(size, bufferOffset).notCancelled() }
            .andThen {
                val sqe = getsqe()
                val pinned = input.pin()

                defer { pinned.unpin() }

                io_uring_prep_send(sqe, handle.actualFd, pinned.addressOf(bufferOffset), size.toULong(), sendFlags)

                submitAndWait(task, SleepingWhy.READ_WRITE) { sqe.setuserid(it) }
            }
    }

    public actual suspend fun fsync(
        handle: IOHandle,
        withMetadata: Boolean
    ): CancellableResourceResult<Empty> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()

                val flags = if (withMetadata) IORING_FSYNC_DATASYNC else 0u
                io_uring_prep_fsync(sqe, handle.actualFd, flags)

                submitAndWait(task, SleepingWhy.FSYNC) { sqe.setuserid(it) }
            }
    }

    // not actually async but lseek() shouldn't (!) block
    public actual suspend fun lseek(
        handle: IOHandle,
        position: Long,
        whence: SeekWhence
    ): CancellableResourceResult<SeekPosition> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val res = lseek64(handle.actualFd, position, whence.value)
                if (res < 0) {
                    Cancellable.failed(posix_errno().toSysError())
                } else {
                    uncancellableCheckpoint(SeekPosition(res))
                }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun fileMetadataAt(
        handle: IOHandle?,
        path: ByteString?,
    ): CancellableResourceResult<PlatformFileMetadata> = memScoped {
        val task = getCurrentTask()
        val out = alloc<statx>()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()

                val dirfd = handle?.actualFd ?: _AT_FDCWD
                val buf = path?.pinnedTerminated() ?: EMPTY_PATH.pin()
                defer { buf.unpin() }

                var flags = 0
                if (path == null) flags = flags.or(_AT_EMPTY_PATH)

                io_uring_prep_statx(
                    sqe, dirfd,
                    buf.addressOf(0),
                    flags,
                    STATX_BASIC_STATS,
                    out.ptr
                )
                submitAndWait<Empty>(task, SleepingWhy.STATX) { sqe.setuserid(it) }
            }
            .andThen {
                Cancellable.ok(
                    PlatformFileMetadata(
                        size = out.stx_size,
                        creationTime = out.stx_ctime.toNs(),
                        modificationTime = out.stx_mtime.toNs(),
                        linkCount = out.stx_nlink,
                        ownerUid = out.stx_uid,
                        ownerGid = out.stx_gid,
                        blockSize = out.stx_blksize,
                        fileMode = out.stx_mode,
                    )
                )
            }
    }

    /**
     * Gets the raw real path for this open file. This may be nonsensical e.g. for temporary files.
     */
    public actual suspend fun realPathOf(handle: IOHandle): CancellableResourceResult<ByteString> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                // this uses /proc/self/fd for reading the link which i believe should never block.
                realPathOfFd(handle.actualFd).notCancelled()
            }
            .andThen { task.uncancellableCheckpoint(it) }
    }

    public actual suspend fun getDirectoryEntries(
        handle: IOHandle,
    ): CancellableResourceResult<List<DirectoryEntry>> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val originalFd = handle.actualFd
                val newFd = dup(originalFd)
                if (newFd < 0) {
                    return posix_errno().toSysResult().notCancelled()
                }

                fcntl(newFd, F_SETFD, FD_CLOEXEC)

                val loop = task.context.eventLoop
                loop.ioWorkerPool.runSyncInThread(cancellable = false, { newFd }) {
                    doReadDir(it)
                }.also { close(newFd) }
            }
    }

    private fun pollFlags(fl: Set<Poll>): UInt {
        var flags = 0
        for (item in fl) {
            when (item) {
                Poll.POLL_READ -> {
                    flags = flags.or(EPOLLIN)
                }
                Poll.POLL_WRITE -> {
                    flags = flags.or(EPOLLOUT)
                }
                // ignore
                else -> {}
            }
        }
        return flags.convert()
    }

    public actual suspend fun pollHandle(
        handle: IOHandle,
        what: Set<Poll>
    ): CancellableResourceResult<PollResult> {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val flags = pollFlags(what)
                val sqe = getsqe()

                io_uring_prep_poll_add(sqe, handle.actualFd, flags)

                submitAndWait(task, SleepingWhy.POLL_ADD) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun makeDirectoryAt(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        permissions: Set<FilePermissions>,
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()

                val dirFd = dirHandle?.actualFd ?: _AT_FDCWD
                val buf = path.pinnedTerminated()
                defer { buf.unpin() }

                var mode = permissions.toMode()
                if (mode == 0U) mode = 511U /* 777 */

                io_uring_prep_mkdirat(sqe, dirFd, buf.addressOf(0), mode)

                submitAndWait(task, SleepingWhy.MKDIR) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun renameAt(
        fromDirHandle: DirectoryHandle?,
        from: ByteString,
        toDirHandle: DirectoryHandle?,
        to: ByteString,
        flags: Set<RenameFlags>
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        var realFlags = 0

        for (flag in flags) {
            realFlags = when (flag) {
                RenameFlags.EXCHANGE -> realFlags.or(_RENAME_EXCHANGE)
                RenameFlags.DONT_REPLACE -> realFlags.or(_RENAME_NOREPLACE)
            }
        }

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                val fromDir = fromDirHandle?.actualFd ?: _AT_FDCWD
                val toDir = toDirHandle?.actualFd ?: _AT_FDCWD

                val fromBuf = from.pinnedTerminated()
                defer { fromBuf.unpin() }
                val toBuf = to.pinnedTerminated()
                defer { toBuf.unpin() }

                io_uring_prep_renameat(
                    sqe,
                    fromDir,
                    fromBuf.addressOf(0),
                    toDir,
                    toBuf.addressOf(0),
                    realFlags.toUInt()
                )

                submitAndWait(task, SleepingWhy.RENAME) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun linkAt(
        fromDirHandle: DirectoryHandle?,
        from: ByteString,
        toDirHandle: DirectoryHandle?,
        to: ByteString,
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                val fromDir = fromDirHandle?.actualFd ?: _AT_FDCWD
                val toDir = toDirHandle?.actualFd ?: _AT_FDCWD

                val fromBuf = from.pinnedTerminated()
                defer { fromBuf.unpin() }
                val toBuf = to.pinnedTerminated()
                defer { toBuf.unpin() }

                io_uring_prep_renameat(
                    sqe,
                    fromDir,
                    fromBuf.addressOf(0),
                    toDir,
                    toBuf.addressOf(0),
                    0
                )

                submitAndWait(task, SleepingWhy.LINK) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun symlinkAt(
        target: ByteString,
        dirHandle: DirectoryHandle?,
        newPath: ByteString,
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()
                val toDir = dirHandle?.actualFd ?: _AT_FDCWD

                val pathBuf = newPath.pinnedTerminated()
                defer { pathBuf.unpin() }

                val targetBuf = newPath.pinnedTerminated()
                defer { targetBuf.unpin() }

                io_uring_prep_symlinkat(sqe, targetBuf.addressOf(0), toDir, pathBuf.addressOf(0))

                submitAndWait(task, SleepingWhy.SYMLINK) { sqe.setuserid(it) }
            }
    }

    @OptIn(Unsafe::class)
    public actual suspend fun unlinkAt(
        dirHandle: DirectoryHandle?,
        path: ByteString,
        removeDir: Boolean,
    ): CancellableResourceResult<Empty> = memScoped {
        val task = getCurrentTask()

        return task.checkIfCancelled()
            .andThen {
                val sqe = getsqe()

                val dirFd = dirHandle?.actualFd ?: _AT_FDCWD
                val buf = path.pinnedTerminated()
                defer { buf.unpin() }

                val flags = if (!removeDir) 0 else _AT_REMOVEDIR

                io_uring_prep_unlinkat(sqe, dirFd, buf.addressOf(0), flags)

                submitAndWait(task, SleepingWhy.UNLINK) { sqe.setuserid(it) }
            }
    }

    override fun close() {
        if (closed) return

        io_uring_queue_exit(ring.ptr)
        close(efd)
        alloca.clear()
        closed = true
    }
}
