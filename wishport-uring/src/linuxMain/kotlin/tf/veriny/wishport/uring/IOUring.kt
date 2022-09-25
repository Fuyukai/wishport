package tf.veriny.wishport.uring

import external.liburing.io_uring
import external.liburing.io_uring_queue_exit
import external.liburing.io_uring_queue_init
import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import tf.veriny.wishport.*
import kotlin.math.abs

public const val DEFAULT_ENTRY_SIZE: UInt = 64U

/**
 * Wrapper around the ``io_uring`` submission and completion queues.
 */
@UringUnsafe
public class IOUring : Closeable {
    public companion object {
        /**
         * Allocates and initialises a new io\_uring with an [entries] number of empty submission
         * slots.
         */
        @UringUnsafe
        public fun open(entries: UInt): ResourceResult<IOUring> {
            val queue = IOUring()
            val res = queue.queueInit(entries)
            if (/* err != nil */res.isFailure) {
                queue.close()
            }

            return res
        }
    }

    private var open = false
    private val alloca = Arena()

    // main i/o ring
    private val ring = alloca.alloc<io_uring>()

    private fun queueInit(entries: UInt): ResourceResult<IOUring> {
        val res = io_uring_queue_init(entries, ring.ptr, 0U)
        if (res < 0) {
            return abs(res).toSysResult()
        }

        open = true
        return Either.ok(this)
    }

    /**
     * Closes the submission queue, freeing up any allocated resources.
     */
    public override fun close() {
        if (!open) return

        io_uring_queue_exit(ring.ptr)
        alloca.clear()
        open = false
    }
}
