/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.CancelScope
import tf.veriny.wishport.core.getIOManager
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.FasterCloseable
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SeekPosition

/**
 * A file handle to a file on the system filehandle.
 */
public class SystemFilesystemHandle
@OptIn(LowLevelApi::class)
internal constructor(
    public override val filesystem: SystemFilesystem,
    public override val raw: IOHandle,
    public override val path: SystemPurePath,
) : FilesystemHandle<SystemPurePath, PlatformFileMetadata>, FasterCloseable {
    public override var closed: Boolean = false
        private set

    public override var closing: Boolean = false
        private set

    override fun provideHandleForClosing(): IOHandle {
        closing = true
        return raw
    }

    override fun notifyClosed() {
        closed = true
    }

    /**
     * Gets the absolute real path for this file descriptor.
     */
    @OptIn(LowLevelApi::class)
    public suspend fun getRealPath(): CancellableResourceResult<SystemPurePath> {
        val io = getIOManager()
        return io.realPathOf(raw)
            .andThen { systemPathFor(it).notCancelled() }
            as CancellableResourceResult<SystemPurePath>
    }

    @OptIn(LowLevelApi::class)
    override suspend fun readInto(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.read(raw, buf, size, fileOffset, bufferOffset)
    }

    @OptIn(LowLevelApi::class)
    override suspend fun writeFrom(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.write(raw, buf, size, fileOffset, bufferOffset)
    }

    @OptIn(LowLevelApi::class)
    override suspend fun flush(withMetadata: Boolean): CancellableResult<Unit, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.fsync(raw, withMetadata = withMetadata).andThen { Cancellable.empty() }
    }

    @OptIn(LowLevelApi::class)
    override suspend fun seek(
        position: Long,
        whence: SeekWhence
    ): CancellableResult<SeekPosition, Fail> {
        if (closed) return Cancellable.failed(AlreadyClosedError)

        val io = getIOManager()
        return io.lseek(raw, position, whence)
    }

    @OptIn(LowLevelApi::class)
    override suspend fun close(): CancellableResult<Unit, Fail> {
        if (closing || closed) return Cancellable.empty()
        closing = true

        return CancelScope(shield = true) {
            val io = getIOManager()
            io.closeHandle(raw).also { closed = true }
        }.andThen { Cancellable.empty() }
    }
}
