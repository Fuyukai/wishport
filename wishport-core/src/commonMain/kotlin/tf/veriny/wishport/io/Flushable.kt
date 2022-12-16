package tf.veriny.wishport.io

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail

/**
 * Interface for any objects that can be flushed to persistent storage.
 */
public interface Flushable {
    /**
     * Flushes the contents of this object to persistent storage. If [withMetadata] is true, then
     * an this will also attempt to flush all of the relevant metadata for the object,
     */
    public suspend fun flush(withMetadata: Boolean = true): CancellableResult<Unit, Fail>
}