package tf.veriny.wishport.internals.io

import tf.veriny.wishport.CancellableResourceResult
import tf.veriny.wishport.Closeable
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.ByteString

// TODO: Rethink if this should be responsible for I/O dispatching itself, or if that functionality
//       should be moved to the event loop, which can then poll this.

/**
 * Responsible for handling I/O within the event loop. This entirely abstracts away how I/O works
 * per-platform, and is responsible internally for its own set of suspended tasks.
 */
@LowLevelApi
@Unsafe
public expect class IOManager : Closeable {
    public companion object {
        public fun default(): IOManager
    }

    /**
     * Peeks off all pending I/O events, and wakes up tasks that would be waiting.
     */
    public fun pollIO()

    /**
     * Waits for I/O forever.
     */
    public fun waitForIO()

    /**
     * Waits for I/O for [timeout] nanoseconds.
     */
    public fun waitForIOUntil(timeout: Long)


    // == actual I/O methods == //
    /**
     * Opens a directory on the real filesystem, returning a directory handle. If [dirHandle] is
     * provided, the directory will be opened relative to the other directory (using openat()
     * semantics).
     */
    public suspend fun openDirectoryHandle(
        dirHandle: DirectoryHandle?,
        path: ByteString
    ): CancellableResourceResult<DirectoryHandle>


}