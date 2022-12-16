/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.collections.toByteString
import tf.veriny.wishport.expect
import tf.veriny.wishport.io.fs.PathComponent.*

@ThreadLocal
private val cachedBuf = FastArrayList<ByteString>()

/**
 * Concatenates all the underlying components of this path into a [ByteString]. The result will be
 * null terminated if [withNullSep] is true.
 */
@OptIn(Unsafe::class)
public fun PurePath<*>.toByteString(withNullSep: Boolean = false): ByteString {
    var size = 0
    // size is *2 as we need extra for the PATH_SEP.
    cachedBuf.clearTo(components.size * 2)

    for (idx in components.indices) {
        when (val comp = components[idx]) {
            is Normal -> {
                cachedBuf.add(comp.data)
                size += comp.data.size
            }

            is Prefix -> TODO()
            is RootDir -> {
                cachedBuf.add(PATH_SEP)
                size += PATH_SEP.size
                continue
            }
            is CurrentDir -> {
                cachedBuf.add(CurrentDir.DOT)
                size += CurrentDir.DOT.size
            }
            is PreviousDir -> {
                cachedBuf.add(PreviousDir.DOTDOT)
                size += PreviousDir.DOTDOT.size
            }
        }

        if (idx != components.size - 1) {
            cachedBuf.add(PATH_SEP)
            size += 1
        }
    }

    // be more efficient than a reduce here
    if (withNullSep) size++
    val out = ByteArray(size)
    var cursor = 0

    for (item in cachedBuf) {
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

/**
 * Like [resolveChild], but takes a string argument.
 */
public fun <Flavour : PurePath<Flavour>> Flavour.resolveChild(name: String): Flavour {
    return resolveChild(name.encodeToByteArray().toByteString()).expect()
}

/**
 * Like [withName], but takes a string argument.
 */
public fun <Flavour : PurePath<Flavour>> Flavour.withName(name: String): Flavour {
    return withName(name.encodeToByteArray().toByteString()).expect()
}

// operator shortcuts
public inline operator fun <Flavour : PurePath<Flavour>> Flavour.div(other: Flavour): Flavour {
    return resolveChild(other)
}
