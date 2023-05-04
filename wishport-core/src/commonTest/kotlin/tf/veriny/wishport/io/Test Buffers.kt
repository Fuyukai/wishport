package tf.veriny.wishport.io

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.assertFailure
import tf.veriny.wishport.assertSuccess
import tf.veriny.wishport.randomByteString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class `Test Buffers` {
    @Test
    fun `Test buffer sanity`() {
        val buf = Buffer()
        buf.writeInt(8)

        assertEquals(4UL, buf.availableBytes(), "Buffer should have a full int")

        val ref = IOResultRef()
        assertSuccess { buf.readInt(ref) }
        assertEquals(8, ref.intValue, "Buffer should have an eight in it")

        assertEquals(0UL, buf.availableBytes(), "Buffer should be empty")
        assertFailure { buf.readInt(ref) }
    }

    @Test
    fun `Test all buffer types`() {
        val ref = IOResultRef()
        val buf = Buffer()

        buf.writeByte(39)
        assertSuccess { buf.readByte(ref) }
        assertEquals(39, ref.byteValue)
        buf.clear()

        buf.writeShort(39)
        assertSuccess { buf.readShort(ref) }
        assertEquals(39, ref.shortValue)
        buf.clear()

        buf.writeInt(39)
        assertSuccess { buf.readInt(ref) }
        assertEquals(39, ref.intValue)
        buf.clear()

        buf.writeLong(39)
        assertSuccess { buf.readLong(ref) }
        assertEquals(39, ref.longValue)
    }

    @Test
    fun `Test buffer endianness`() {
        val ref = IOResultRef()
        val buf = Buffer()

        buf.writeShort(0x39_93)
        assertSuccess { buf.readShortLE(ref) }
        assertEquals(0x93_39.toShort(), ref.shortValue, "Endianness should be flipped")
    }

    @OptIn(Unsafe::class)
    @Test
    fun `Test large buffer writes`() {
        val random = Random.randomByteString(2048).unwrap()
        val buf = Buffer()
        buf.writeAll(random)

        assertEquals(
            random.size.toULong(), buf.availableBytes(),
            "buffer should have entirely consumed provided array"
        )

        // 128 is bigger than the buffer's node size...
        // make sure to update this if that changes.
        for (chunk in 0 until 2048 step 128) {
            val b = ByteArray(128)
            assertSuccess { buf.readIntoUpto(b) }
            val slice = random.sliceArray(chunk until chunk+128)
            assertContentEquals(slice, b)
        }

        assertEquals(0UL, buf.availableBytes())
    }
}
