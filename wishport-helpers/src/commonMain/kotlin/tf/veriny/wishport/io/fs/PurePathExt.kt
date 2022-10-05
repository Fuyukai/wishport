/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.fs.PathComponent.*

/**
 * Concatenates all the underlying components of this path into a [ByteString].
 */
public fun PurePath<*>.toByteString(): ByteString {
    // we take some liberties with internal apis!
    // TODO: once we get a real byte buffer like, we can avoid using the internal apis altogether
    val buf = ArrayList<ByteString>()

    for (idx in components.indices) {
        when (val comp = components[idx]) {
            is Prefix -> TODO()
            is RootDir -> {
                buf.add(PATH_SEP)
                continue
            }
            is CurrentDir -> buf.add(CurrentDir.DOT)
            is PreviousDir -> buf.add(PreviousDir.DOTDOT)
            is Normal -> buf.add(comp.data)
        }

        if (idx != components.size - 1) buf.add(PATH_SEP)
    }

    return buf.reduce { acc, bs -> acc + bs }
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
