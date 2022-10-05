package tf.veriny.wishport.collections

import kotlinx.cinterop.Pinned
import kotlinx.cinterop.pin
import tf.veriny.wishport.annotations.Unsafe

public actual class ByteString
private actual constructor(private val backing: ByteArray) : Collection<Byte> {
    public actual companion object {
        /**
         * Creates a new [ByteString] from a [ByteArray].
         */
        public actual operator fun invoke(backing: ByteArray): ByteString =
            ByteString(backing.copyOf())

        /**
         * Creates a new [ByteString] from a [String].
         */
        public actual operator fun invoke(s: String): ByteString =
            ByteString(s.encodeToByteArray(throwOnInvalidSequence = false))

        /**
         * Creates a new [ByteString] from a [ByteArray].
         */
        @Unsafe
        public actual fun uncopied(ba: ByteArray): ByteString =
            ByteString(ba)
    }

    override val size: Int
        get() = backing.size

    override fun contains(element: Byte): Boolean {
        return backing.contains(element)
    }

    override fun iterator(): Iterator<Byte> {
        return backing.iterator()
    }

    override fun isEmpty(): Boolean {
        return backing.isEmpty()
    }

    override fun containsAll(elements: Collection<Byte>): Boolean {
        return elements.all { backing.contains(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ByteString) return false
        if (other === this) return true

        return other.backing.contentEquals(backing)
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    // useful methods
    public actual operator fun get(idx: Int): Byte? {
        if (idx > size) return null
        return backing[idx]
    }

    public actual fun getUnsafe(idx: Int): Byte {
        return backing[idx]
    }

    /**
     * Gets a pinned reference to the underlying [ByteArray]. It's your job to clean up.
     */
    @Unsafe
    public fun pinnedTerminated(): Pinned<ByteArray> {
        if (backing.last() == (0).toByte()) {
            return backing.pin()
        }

        val copy = backing.copyInto(ByteArray(backing.size + 1) { 0 })
        return copy.pin()
    }
}