/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.fs.PathComponent.*

/**
 * Concatenates all the underlying components of this path into a [ByteString]. The result will be
 * null terminated if [withNullSep] is true.
 */
@OptIn(Unsafe::class)
public fun PurePath<*>.toByteString(withNullSep: Boolean = false): ByteString {
    // we take some liberties with internal apis!
    // TODO: once we get a real byte buffer like, we can avoid using the internal apis altogether
    var size = 0
    val buf = ArrayList<ByteString>(components.size)

    for (idx in components.indices) {
        when (val comp = components[idx]) {
            is Prefix -> TODO()
            is RootDir -> {
                buf.add(PATH_SEP)
                size += PATH_SEP.size
                continue
            }
            is CurrentDir -> {
                buf.add(CurrentDir.DOT)
                size += CurrentDir.DOT.size
            }
            is PreviousDir -> {
                buf.add(PreviousDir.DOTDOT)
                size += PreviousDir.DOTDOT.size
            }
            is Normal -> {
                buf.add(comp.data)
                size += comp.data.size
            }
        }

        if (idx != components.size - 1) {
            buf.add(PATH_SEP)
            size += 1
        }
    }

    // be more efficient than a reduce here
    if (withNullSep) size++
    val out = ByteArray(size)
    var cursor = 0

    for (item in buf) {
        item.unwrap().copyInto(out, destinationOffset = cursor)
        cursor += item.size
    }

    return ByteString.uncopied(out)
}

/**
 * The filename for this [PurePath]. May return null if this has no filename
 * (e.g. is the root directory).
 */
public inline val PurePath<*>.fileName: ByteString? get() {
    return (components.last() as? PathComponent.Normal)?.data
}

/**
 * Checks if this path is the parent of the [other] path provided.
 */
/* @InlineOnly */
public inline fun <Flavour : PurePath<Flavour>> Flavour.isParentOf(
    other: Flavour
): Boolean {
    return other.isChildOf(this)
}
