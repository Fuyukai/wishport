package tf.veriny.wishport.internals

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.andThen
import tf.veriny.wishport.io.fs.SystemFilesystemHandle

public actual suspend fun SystemFilesystemHandle.getIdealBlockSize(): CancellableResult<UInt, Fail> {
    return filesystem.getFileMetadata(this).andThen { Cancellable.ok(it.blockSize) }
}