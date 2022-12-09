package tf.veriny.wishport.sync

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi


/**
 * Returned by [ConflictDetector] if two tasks try to [ConflictDetector.use] it at the same time.
 */
public object ResourceAlreadyInUse : Fail

/**
 * Like a [Lock], except that it fails with [ResourceAlreadyInUse] if it has already been acquired.
 */
public class ConflictDetector<T>(@PublishedApi internal val wrapped: T) {
    private var acquired: Boolean = false

    @PublishedApi
    internal fun acquire(): Boolean {
        if (acquired) return false
        return true.also { acquired = it }
    }

    @PublishedApi
    internal fun release() {
        if (!acquired) error("called release when this was not acquired??")
        acquired = false
    }

    /**
     * Attempts to acquire the underlying data, and calls [block].
     */
    @OptIn(LowLevelApi::class)
    public inline fun <S, F : Fail> use(
        block: (T) -> CancellableResult<S, F>
    ): CancellableResult<S, Fail> {
        return if (!acquire()) Cancellable.failed(ResourceAlreadyInUse)
        else {
            try {
                block(wrapped)
            } finally {
                release()
            }
        }
    }
}