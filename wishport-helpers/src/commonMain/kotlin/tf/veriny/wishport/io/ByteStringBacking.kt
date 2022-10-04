/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

/**
 * A sealed type over the possible backing stores for a [ByteString].
 */
internal sealed interface ByteStringBacking {
    /** The size of this bytestring. */
    val size: Int

    /** Gets a single byte at the specified index. */
    fun get(idx: Int): Byte?

    // used to get pointers
    /** Unwraps the underlying byte array from this byte string. */
    fun unwrap(): ByteArray

    fun contains(other: Byte): Boolean

    fun containsAll(elements: Collection<Byte>): Boolean {
        return elements.all { contains(it) }
    }

    /**
     * Decodes to a surrogateescape String.
     */
    fun decode(): String
}

/** A holder that just uses an array directly. */
internal class ByteArrayHolder(private val arr: ByteArray) : ByteStringBacking {
    override val size: Int
        get() = arr.size

    override fun get(idx: Int): Byte? {
        if (idx < 0 || idx >= size) return null
        return arr[idx]
    }

    override fun unwrap(): ByteArray {
        return arr
    }

    override fun contains(other: Byte): Boolean {
        return arr.contains(other)
    }

    override fun decode(): String {
        return arr.decodeToString(throwOnInvalidSequence = false)
    }
}

/** A holder that holds a slice of the underlying array. */
internal class ByteArraySliceHolder(
    private val parent: ByteStringBacking,
    private val start: Int,
    private val end: Int
) : ByteStringBacking {
    override val size: Int
        get() = (end - start)

    private fun checkSize(idx: Int): Boolean {
        if (idx > size) return false
        if (idx >= end || idx < start) return false
        return true
    }

    override fun get(idx: Int): Byte? {
        if (!checkSize(idx)) return null
        return parent.get(idx + start)
    }

    override fun unwrap(): ByteArray {
        return parent.unwrap().copyOfRange(start, end)
    }

    override fun contains(other: Byte): Boolean {
        return unwrap().contains(other)
    }

    override fun containsAll(elements: Collection<Byte>): Boolean {
        val ba = unwrap()
        return elements.all { ba.contains(it) }
    }

    override fun decode(): String {
        return unwrap().decodeToString(throwOnInvalidSequence = false)
    }
}
