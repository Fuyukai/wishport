/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.collections

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString.Companion.uncopied

// TODO: we need ByteStringBuilder, or a Buffer, or anything like that.

/**
 * An immutable sequence of bytes in a string-like container.
 */
public expect class ByteString
private constructor(backing: ByteArray) : Collection<Byte> {
    public companion object {
        public val EMPTY: ByteString

        /**
         * Creates a new [ByteString] from a [ByteArray].
         */
        public operator fun invoke(backing: ByteArray): ByteString

        /**
         * Creates a new [ByteString] from a [String].
         */
        public operator fun invoke(s: String): ByteString

        /**
         * Creates a new [ByteString] from a [ByteArray].
         */
        @Unsafe
        public fun uncopied(ba: ByteArray): ByteString
    }

    /** Gets the byte at index [idx]. */
    public operator fun get(idx: Int): Byte?

    /**
     * Gets the byte at index [idx]. This is unsafe as it may throw an exception.
     */
    @Unsafe
    public fun getUnsafe(idx: Int): Byte

    @Unsafe
    internal fun unwrap(): ByteArray

    /**
     * Appends another [ByteString] to this one, producing a new ByteString that is independent
     * of the other two.
     */
    public operator fun plus(other: ByteString): ByteString
}

public inline fun b(s: String): ByteString = ByteString(s)

/**
 * Slices this [ByteString].
 */
@OptIn(Unsafe::class)
public fun ByteString.slice(from: Int, to: Int): ByteString? {
    if (from < 0 || to >= size) return null

    val ba = ByteArray(to - from)
    for (i in from until to) {
        ba[i] = getUnsafe(i)
    }

    return uncopied(ba)
}

/**
 * Checks if this [ByteString] starts with another [ByteString].
 */
@OptIn(Unsafe::class)
public fun ByteString.startsWith(other: ByteString): Boolean {
    if (other.size > size) return false
    for (i in other.indices) {
        if (other.getUnsafe(i) != getUnsafe(i)) return false
    }

    return true
}

/**
 * Checks if this [ByteString] starts with the specified [other] byte.
 */
@OptIn(Unsafe::class)
public fun ByteString.startsWith(other: Byte): Boolean {
    // what?
    if (isEmpty()) return false

    return getUnsafe(0) == other
}

// TODO: This is not very efficient.
//  I believe a proper search alg would be faster. But that's for another day.
/**
 * Splits a [ByteString] by the specified [delim].
 */
public fun ByteString.split(delim: ByteString): List<ByteString> {
    // final output
    val working = ArrayList<ByteString>(size / delim.size)
    // current processing
    val current = ByteArray(size)
    // pointer to the head of the current processing
    var currentCursor = 0
    // matched count to the delimiter, used to chop the tail off
    var matched = 0

    // cool function!
    for (byt in this) {
        // always copy the byte to the current working array
        current[currentCursor] = byt
        currentCursor += 1

        // delim check
        if (byt == delim[matched]) {
            matched += 1
        } else {
            matched = 0
        }

        // if the match count is the same as the delim, we have fully matched the delimiter
        // in which case, we chop off the delimiter, then copy the working array
        if (matched == delim.size) {
            val copy = ByteString(current.copyOfRange(0, currentCursor - matched))
            currentCursor = 0
            matched = 0
            current.fill(0)

            working.add(copy)
        }
    }

    // exited the loop, add anything left in the current cursor to the list
    // (as it didn't match fully)
    if (currentCursor > 0) {
        val copy = ByteString(current.copyOfRange(0, currentCursor))
        working.add(copy)
    }
    return working
}

/**
 * Returns the index of the first byte [b] in the [ByteString], or -1 if it is not found.
 */
@OptIn(Unsafe::class)
public fun ByteString.find(b: Byte): Int {
    for (idx in this.indices) {
        if (getUnsafe(idx) == b) return idx
    }

    return -1
}

/**
 * Joins an iterable of [ByteString] together with the specified [delim].
 */
@OptIn(Unsafe::class)
public fun Collection<ByteString>.join(delim: ByteString): ByteString {
    val size = (delim.size * (this.size - 1)) + this.sumOf { it.size }
    val final = ByteArray(size)
    var cursor = 0

    val it = iterator()

    for (part in it) {
        for (b in part) {
            final[cursor] = b
            cursor += 1
        }

        // clever check for the last item
        // hasNext() will return false if this is the last one
        // and we don't want a trailing delimiter
        if (it.hasNext()) {
            for (b in delim) {
                final[cursor] = b
                cursor += 1
            }
        }
    }

    return uncopied(final)
}

/**
 * An array corresponding to the hex alphabet.
 */
public val HEX_ALPHABET: Array<Char> =
    arrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    )

/**
 * Creates a new hex-encoded string from this ByteString.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public fun ByteString.hexlify(): String {
    if (isEmpty()) return ""

    val buf = StringBuilder(this.size * 2)
    for (byte in this) {
        val ubyte = byte.toUByte()
        val upper = HEX_ALPHABET[((ubyte and 0xF0u).toInt()).ushr(4)]
        val lower = HEX_ALPHABET[(ubyte and 0x0Fu).toInt()]
        buf.append(upper)
        buf.append(lower)
    }

    return buf.toString()
}

public fun ByteString.escapedString(): String {
    return joinToString("") {
        if (it in 32..126) it.toInt().toChar().toString()
        else "\\x" + it.toUByte().toString(16).padStart(2, '0')
    }
}

public fun ByteArray.toByteString(): ByteString = ByteString(this)

@OptIn(Unsafe::class)
public fun Collection<Byte>.toByteString(): ByteString = uncopied(toByteArray())
