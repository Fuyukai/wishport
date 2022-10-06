/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.getIOManager
import tf.veriny.wishport.internals.io.ByteCountResult
import tf.veriny.wishport.internals.io.ReadableHandle
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

/**
 * A file handle to a file on the system filehandle.
 */
public class SystemFileHandle
@OptIn(LowLevelApi::class)
internal constructor(
    public override val filesystem: Filesystem<SystemPurePath>,
    public override val raw: ReadableHandle,
    public override val path: SystemPurePath,
) : FileHandle<SystemPurePath> {

    @Unsafe
    override suspend fun openRelative(
        path: SystemPurePath,
        mode: FileOpenMode,
        flags: Set<FileOpenFlags>,
    ): CancellableResult<FileHandle<SystemPurePath>, Fail> {
        return filesystem.getRelativeFileHandle(this, path, mode, flags)
    }

    @OptIn(Unsafe::class, LowLevelApi::class)
    override suspend fun readInto(
        buf: ByteArray,
        size: UInt,
        bufferOffset: Int,
        fileOffset: ULong
    ): CancellableResult<ByteCountResult, Fail> {
        val io = getIOManager()
        return io.read(raw, buf, size, fileOffset, bufferOffset)
    }

    override fun close() {
        raw.close()
    }
}
