/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.streams

import tf.veriny.wishport.*
import tf.veriny.wishport.io.ByteCountResult
import tf.veriny.wishport.io.FileLikeHandle

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
 * Implements [writeAll] semantics over a [FileLikeHandle]. This takes a final callback parameter
 * which is called when the stream is damaged.
 */
public suspend inline fun FileLikeHandle.writeAll(
    buffer: ByteArray,
    byteCount: UInt,
    bufferOffset: Int,
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
