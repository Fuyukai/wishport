/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)
@file:Suppress("unused", "NOTHING_TO_INLINE")

package tf.veriny.wishport

import platform.builtin.builtin_bswap16
import platform.builtin.builtin_bswap32
import platform.builtin.builtin_bswap64

/**
 * Gets the upper byte of this Int.
 */
/* @InlineOnly */
public inline val Int.upperByte: Int
    get() = (this ushr 24) and 0xFF

/**
 * Gets the upper byte of this UInt.
 */
/* @InlineOnly */
public inline val UInt.upperByte: UInt
    get() = (this shr 24) and 0xFFu

/**
 * Gets the lower byte of this Int.
 */
/* @InlineOnly */
public inline val Int.lowerByte: Int
    get() = this and 0xFF

/**
 * Gets the lower byte of this UInt.
 */
/* @InlineOnly */
public inline val UInt.lowerByte: UInt
    get() = this and 0xFFu

/**
 * Gets the second byte of this Int.
 */
/* @InlineOnly */
public inline val Int.byte2: Int
    get() = (this ushr 16) and 0xFF

/**
 * Gets the second byte of this UInt.
 */
/* @InlineOnly */
public inline val UInt.byte2: UInt
    get() = (this shr 16) and 0xFFu

/**
 * Gets the third byte of this Int.
 */
/* @InlineOnly */
public inline val Int.byte3: Int
    get() = (this ushr 8) and 0xFF

/**
 * Gets the third byte of this UInt.
 */
/* @InlineOnly */
public inline val UInt.byte3: UInt
    get() = (this shr 8) and 0xFFu

/**
 * Gets the upper byte of this Long.
 */
/* @InlineOnly */
public inline val Long.upperByte: Long
    get() = ((this.toULong()) and 0xFF00000000000000UL).toLong()

/**
 * Gets the lower byte of this Long.
 */
/* @InlineOnly */
public inline val Long.lowerByte: Long
    get() = (this and 0x00000000000000FF)

/* @InlineOnly */
public inline val Long.swapped: Long get() = (builtin_bswap64(this))

/* @InlineOnly */
public inline val Int.swapped: Int get() = (builtin_bswap32(this))

/* @InlineOnly */
public inline val Short.swapped: Short get() = (builtin_bswap16(this))

// Number --> ByteArray
/**
 * Decodes this int into a [ByteArray] in big endian mode.
 */
/* @InlineOnly */
public inline fun Int.toByteArray(): ByteArray {
    return byteArrayOf(upperByte.toByte(), byte2.toByte(), byte3.toByte(), lowerByte.toByte())
}

/**
 * Decodes this int into a [ByteArray] in little endian mode.
 */
/* @InlineOnly */
public inline fun Int.toByteArrayLE(): ByteArray {
    return byteArrayOf(lowerByte.toByte(), byte3.toByte(), byte2.toByte(), upperByte.toByte())
}

/**
 * Decodes this uint into a ByteArray in big endian mode.
 */
/* @InlineOnly */
@OptIn(ExperimentalUnsignedTypes::class) // ?
public inline fun UInt.toByteArray(): ByteArray {
    return byteArrayOf(upperByte.toByte(), byte2.toByte(), byte3.toByte(), lowerByte.toByte())
}

/**
 * Decodes this int into a [ByteArray] in little endian mode.
 */
/* @InlineOnly */
public inline fun UInt.toByteArrayLE(): ByteArray {
    return byteArrayOf(lowerByte.toByte(), byte3.toByte(), byte2.toByte(), upperByte.toByte())
}

/**
 * Decodes this long into a [ByteArray] in big endian mode.
 */
/* @InlineOnly */
public inline fun Long.toByteArray(): ByteArray {
    return byteArrayOf(
        ((this ushr 56) and 0xffL).toByte(),
        ((this ushr 48) and 0xffL).toByte(),
        ((this ushr 40) and 0xffL).toByte(),
        ((this ushr 32) and 0xffL).toByte(),
        ((this ushr 24) and 0xffL).toByte(),
        ((this ushr 16) and 0xffL).toByte(),
        ((this ushr 8) and 0xffL).toByte(),
        ((this) and 0xffL).toByte()
    )
}

/**
 * Decodes this long into a [ByteArray] in little endian mode.
 */
/* @InlineOnly */
public inline fun Long.toByteArrayLE(): ByteArray {
    return byteArrayOf(
        ((this) and 0xffL).toByte(),
        ((this ushr 8) and 0xffL).toByte(),
        ((this ushr 16) and 0xffL).toByte(),
        ((this ushr 24) and 0xffL).toByte(),
        ((this ushr 32) and 0xffL).toByte(),
        ((this ushr 40) and 0xffL).toByte(),
        ((this ushr 48) and 0xffL).toByte(),
        ((this ushr 56) and 0xffL).toByte()
    )
}

/**
 * Decodes this ulong into a [ByteArray] in big endian mode.
 */
public inline fun ULong.toByteArray(): ByteArray {
    return byteArrayOf(
        ((this shr 56) and 0xffUL).toByte(),
        ((this shr 48) and 0xffUL).toByte(),
        ((this shr 40) and 0xffUL).toByte(),
        ((this shr 32) and 0xffUL).toByte(),
        ((this shr 24) and 0xffUL).toByte(),
        ((this shr 16) and 0xffUL).toByte(),
        ((this shr 8) and 0xffUL).toByte(),
        ((this) and 0xffUL).toByte()
    )
}

/**
 * Decodes this long into a [ByteArray] in little endian mode.
 */
/* @InlineOnly */
public inline fun ULong.toByteArrayLE(): ByteArray {
    return byteArrayOf(
        ((this) and 0xffUL).toByte(),
        ((this shr 8) and 0xffUL).toByte(),
        ((this shr 16) and 0xffUL).toByte(),
        ((this shr 24) and 0xffUL).toByte(),
        ((this shr 32) and 0xffUL).toByte(),
        ((this shr 40) and 0xffUL).toByte(),
        ((this shr 48) and 0xffUL).toByte(),
        ((this shr 56) and 0xffUL).toByte()
    )
}

// misc helpers
/**
 * Creates a new int with all of the specified [flag] values OR'd together.
 */
public fun flags(vararg flag: Int): Int {
    var acc = 0
    for (f in flag) {
        acc = acc.or(f)
    }
    return acc
}

/**
 * Creates a new uint with all of the specified [flag] values OR'd together.
 */
public fun flags(vararg flag: UInt): UInt {
    var acc = 0u
    for (f in flag) {
        acc = acc.or(f)
    }
    return acc
}

/**
 * Checks if [input] has bits from [flag] set.
 */
public fun flagged(input: Int, flag: Int): Boolean {
    return input.and(flag) != 0
}

/**
 * Checks if [input] has bits from [flag] set.
 */
public fun flagged(input: Int, flag: UInt): Boolean {
    return input.toUInt().and(flag) != 0u
}

/**
 * Checks if [input] has bits from [flag] set.
 */
public fun flagged(input: UInt, flag: Int): Boolean {
    return input.and(flag.toUInt()) != 0u
}

/**
 * Checks if [input] has bits from [flag] set.
 */
public fun flagged(input: UInt, flag: UInt): Boolean {
    return input.and(flag) != 0u
}

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [Long].
 */
public fun Long.bit(idx: Int): Boolean {
    return (this.shr(idx - 1).and(1)) == 1L
}

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [Long].
 */
public fun ULong.bit(idx: Int): Boolean = toLong().bit(idx)

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [Int].
 */
public fun Int.bit(idx: Int): Boolean = toLong().bit(idx)

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [UInt].
 */
public fun UInt.bit(idx: Int): Boolean = toLong().bit(idx)

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [Byte].
 */
public fun Byte.bit(idx: Int): Boolean = toLong().bit(idx)

/**
 * Checks if the bit [idx] (one-indexed) is flagged in this [UByte].
 */
public fun UByte.bit(idx: Int): Boolean = toLong().bit(idx)

@Suppress("ConvertTwoComparisonsToRangeCheck")
public fun Char.isHex(): Boolean {
    return ((this >= 'a' && this <= 'f') || (this >= 'A' && this <= 'F') || (this >= '0' && this <= '9'))
}

/**
 * Converts a hexadecimal char into an [Int].
 */
@Suppress("ConvertTwoComparisonsToRangeCheck")
public fun Char.toIntHex(): Int {
    return if (this >= 'a' && this <= 'f') {
        code - 87 // 'a' is ordinal 97
    } else if (this >= 'A' && this <= 'F') {
        code - 55 // 'f' is ordinal 65
    } else if (this >= '0' && this <= '9') {
        code - 48 // '0' is ordinal 48
    } else {
        throw IllegalArgumentException("Not a hexadecimal digit: $this")
    }
}
