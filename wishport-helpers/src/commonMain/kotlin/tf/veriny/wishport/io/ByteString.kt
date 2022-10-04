package tf.veriny.wishport.io

import tf.veriny.wishport.annotations.Unsafe

/**
 * An immutable string of binary characters.
 */
public class ByteString
private constructor(private val backing: ByteStringBacking) : Collection<Byte> {
    public companion object {
        /**
         * Creates a new [ByteString] from the passed ByteArray. This will copy the provided array.
         */
        public fun from(arr: ByteArray): ByteString {
            val copy = arr.copyOf()
            val ba = ByteArrayHolder(copy)
            return ByteString(ba)
        }

        /**
         * Creates a new [ByteString] from the specified
         */
        public fun from(str: String): ByteString {
            val encoded = str.encodeToByteArray()
            return ByteString(ByteArrayHolder(encoded))
        }
    }

    override val size: Int get() = backing.size

    override fun isEmpty(): Boolean {
        return backing.size == 0
    }

    override fun contains(element: Byte): Boolean {
        return backing.contains(element)
    }

    override fun containsAll(elements: Collection<Byte>): Boolean {
        return backing.containsAll(elements)
    }

    @OptIn(Unsafe::class)
    override fun iterator(): Iterator<Byte> {
        return unwrap().iterator()
    }

    /**
     * Unwraps the underlying [ByteArray] for this [ByteString]. This is unsafe as it may expose
     * the internal array directly, violating immutability.
     */
    @Unsafe
    public fun unwrap(): ByteArray {
        return backing.unwrap()
    }

    /**
     * Creates a new [ByteArray] with a null terminator, for passing to C functions that expect a
     * C string.
     */
    public fun toNullTerminated(): ByteArray {
        val copy = unwrapCopy()
        // already null-terminated
        if (copy.last() == 0.toByte()) return copy

        val newSize = size + 1
        val realArr = ByteArray(newSize)
        copy.copyInto(realArr)
        return realArr
    }

    /**
     * Unwraps this [ByteString] and returns a copy of the underlying data.
     */
    @OptIn(Unsafe::class)
    public fun unwrapCopy(): ByteArray {
        return unwrap().copyOf()
    }

    // currentt impl doesnt copy for speed
    /**
     * Gets a slice of this [ByteString]. This may return null if the indexes are invalid.
     */
    public fun slice(from: Int, to: Int): ByteString? {
        if (from < 0 || to > size) return null

        return ByteString(ByteArraySliceHolder(backing, from, to))
    }
}