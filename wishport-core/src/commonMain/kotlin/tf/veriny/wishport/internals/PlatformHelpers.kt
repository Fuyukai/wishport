package tf.veriny.wishport.internals

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.io.fs.SystemFilesystemHandle

/**
 * Gets the ideal block size for the specified file, for usage with buffered i/o.
 */
@LowLevelApi
public expect suspend fun SystemFilesystemHandle.getIdealBlockSize(): CancellableResult<UInt, Fail>