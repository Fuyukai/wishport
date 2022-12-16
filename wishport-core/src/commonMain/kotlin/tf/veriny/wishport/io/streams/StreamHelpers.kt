/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.streams

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.FileLikeHandle

/**
 * Reads from this FileLikeHandle until it returns EOF. You can pass [hintSize] to set the size of
 * the buffer chunks (e.g. for a regular file this should be stx_size).
 */
@OptIn(Unsafe::class)
public suspend fun FileLikeHandle.readUntilEof(
    hintSize: UInt = 8096U,
): CancellableResult<ByteArray, Fail> = Imperatavize.cancellable {
    val bufferChunks = FastArrayList<ByteArray>()
    var lastBuffer = ByteArray(hintSize.toInt())
    var lastReadCount = 0U

    while (true) {
        val readAmount = lastBuffer.size.toUInt() - lastReadCount
        val readOffset = lastReadCount.toInt()

        val data = readInto(lastBuffer, size = readAmount, bufferOffset = readOffset).q()
        when (val count = data.count) {
            0U -> {
                // EOF
                break
            }

            hintSize -> {
                // full chunk, swap out buffer
                bufferChunks.add(lastBuffer)
                lastBuffer = ByteArray(hintSize.toInt())
                lastReadCount = 0U
            }

            else -> {
                // uh oh
                lastReadCount += count
            }
        }
    }

    // micro opt: for files where the hint was the file size, directly returned the allocated array
    // instead of copying. this avoids an allocation and a memmove.
    if (lastReadCount == 0U && bufferChunks.size == 1) {
        return@cancellable bufferChunks[0]
    }

    val perChunk = hintSize.toInt()
    val out = if (lastReadCount > 0U) {
        ByteArray((bufferChunks.size * perChunk) + lastReadCount.toInt())
    } else {
        ByteArray(bufferChunks.size * perChunk)
    }

    for ((count, chunk) in bufferChunks.withIndex()) {
        chunk.copyInto(out, destinationOffset = count * perChunk)
    }

    if (lastReadCount > 0U) {
        lastBuffer.copyInto(
            out,
            destinationOffset = bufferChunks.size * perChunk,
            startIndex = 0,
            endIndex = lastReadCount.toInt()
        )
    }

    out
}

/**
 * Implements [writeMost] semantics over a [FileLikeHandle].
 */
public suspend fun FileLikeHandle.writeMost(
    buffer: ByteArray,
    byteCount: UInt = buffer.size.toUInt(),
    bufferOffset: Int = 0
): CancellableResult<ByteCountResult, Fail> {
    if (closed) return Cancellable.failed(AlreadyClosedError)

    var runningTotal = 0U

    while (runningTotal < byteCount) {
        val result = this.writeFrom(
            buffer, byteCount - runningTotal, bufferOffset, ULong.MAX_VALUE
        )
        if (result.isCancelled) {
            return if (runningTotal > 0U) Cancellable.ok(ByteCountResult(runningTotal))
            else result
        } else if (result.isFailure) {
            return if (runningTotal > 0U) {
                Cancellable.failed(WriteMostFailed(result.getFailure()!!, byteCount))
            } else result
        } else {
            runningTotal += result.get()!!.count
        }
    }

    return Cancellable.ok(ByteCountResult(runningTotal))
}

/**
 * Like [writeMost], but uses a [ByteString] instead.
 */
@OptIn(Unsafe::class)
public suspend fun FileLikeHandle.writeMost(
    bs: ByteString
): CancellableResult<ByteCountResult, Fail> {
    return writeMost(bs.unwrap())
}

/**
 * Implements [writeAll] semantics over a [FileLikeHandle]. This takes a final callback parameter
 * which is called when the stream is damaged, which is useful for implementing streams.
 */
public suspend fun FileLikeHandle.writeAll(
    buffer: ByteArray,
    byteCount: UInt = buffer.size.toUInt(),
    bufferOffset: Int = 0,
    cb: () -> Unit,
): CancellableResult<Unit, Fail> {
    if (closed) return Cancellable.failed(AlreadyClosedError)

    var runningTotal = 0U

    while (runningTotal < byteCount) {
        val result = this.writeFrom(
            buffer, byteCount - runningTotal, bufferOffset, ULong.MAX_VALUE
        )

        // safe cast, the <Unit> success is not part of us
        @Suppress("UNCHECKED_CAST")
        when {
            result.isSuccess -> {
                runningTotal += result.get()!!.count
            }

            else -> {
                cb()
                return result as CancellableResult<Unit, Fail>
            }
        }
    }

    return Cancellable.empty()
}

/**
 * Like [writeAll], but without the need to provide a callback.
 */
public suspend inline fun FileLikeHandle.writeAll(
    buffer: ByteArray,
    byteCount: UInt = buffer.size.toUInt(),
    bufferOffset: Int = 0,
): CancellableResult<Unit, Fail> {
    return writeAll(buffer, byteCount, bufferOffset) {}
}

/**
 * Like [writeAll], but uses a [ByteString] instead.
 */
@OptIn(Unsafe::class)
public suspend fun FileLikeHandle.writeAll(
    bs: ByteString,
    cb: () -> Unit
): CancellableResult<Unit, Fail> {
    return writeAll(bs.unwrap(), cb = cb)
}

/**
 * Like [writeAll], but uses a [ByteString] instead.
 */
@OptIn(Unsafe::class)
public suspend fun FileLikeHandle.writeAll(
    bs: ByteString
): CancellableResult<Unit, Fail> {
    return writeAll(bs.unwrap()) {}
}
