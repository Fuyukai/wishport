/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.Cancellable
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.andThen
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.streams.PartialWriteStream
import tf.veriny.wishport.io.streams.ReadStream
import tf.veriny.wishport.io.streams.WriteStream

/**
 * Reads up to [maxSize] bytes from this [ReadStream], returning a [ByteString]. This will return
 * an empty string on end-of-file.
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun ReadStream.readUpto(
    maxSize: UInt
): CancellableResult<ByteString, Fail> {
    val size = maxSize.toInt()
    val buffer = ByteArray(size)

    return readIntoUpto(buffer)
        .andThen {
            when (val count = it.count) {
                0U -> Cancellable.ok(ByteString.EMPTY)
                maxSize -> Cancellable.ok(ByteString.uncopied(buffer))
                else -> {
                    val copy = buffer.copyOfRange(0, count.toInt())
                    Cancellable.ok(ByteString.uncopied(copy))
                }
            }
        }
}

/**
 * Writes the entirety of [bs] into this [WriteStream].
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public suspend fun WriteStream.writeAll(bs: ByteString): CancellableResult<Unit, Fail> {
    val unwrapped = bs.unwrap()
    return writeAll(unwrapped)
}

/**
 * Attempts to write most, if not all, of [bs] into this [PartialWriteStream].
 */
@ProvisionalApi
@OptIn(Unsafe::class)
public suspend fun PartialWriteStream.writeMost(
    bs: ByteString
): CancellableResult<ByteCountResult, Fail> {
    return writeMost(bs.unwrap())
}
