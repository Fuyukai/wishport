package tf.veriny.wishport.io

/**
 * A performance hack for reading/writing values to and from a buffer without incurring the cost
 * of boxing primitives.
 */
public class IOResultRef(public var value: ULong = 0UL) : Comparable<Number> {
    override fun toString(): String {
        return value.toString()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is IOResultRef) {
            return other.value == value
        }

        if (other is Number) {
            return value == other.toLong().toULong()
        }

        return false
    }

    override fun compareTo(other: Number): Int {
        return value.compareTo(other.toLong().toULong())
    }
}

public var IOResultRef.longValue: Long
    get() = value.toLong()
    set(v) { value = v.toULong() }

public var IOResultRef.uintValue: UInt
    get() = value.toUInt()
    set(v) { value = v.toULong() }

public var IOResultRef.intValue: Int
    get() = value.toInt()
    set(v) { value = v.toULong() }

public var IOResultRef.ushortValue: UShort
    get() = value.toUShort()
    set(v) { value = v.toULong() }

public var IOResultRef.shortValue: Short
    get() = value.toShort()
    set(v) { value = v.toULong() }

public var IOResultRef.ubyteValue: UByte
    get() = value.toUByte()
    set(v) { value = v.toULong() }

public var IOResultRef.byteValue: Byte
    get() = value.toByte()
    set(v) { value = v.toULong() }




