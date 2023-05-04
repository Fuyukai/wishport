package tf.veriny.wishport.io

import tf.veriny.wishport.*
import kotlin.math.min

// Implementation notes: This is a simple linked list queue of ByteArray nodes.
// TODO: look at recycling the Node instances, like okio buffers?

private const val NODE_SIZE = 64

/**
 * An in-memory buffer that allows convenient reading/writing methods.
 */
public actual class Buffer(public val nodeSize: Int = NODE_SIZE) {
    // these are internal as the I/O manager needs to reach in and fuck with nodes
    internal inner class Node {
        var read: Int = 0
        var write: Int = 0

        val arr = ByteArray(nodeSize)

        // note: these may be non-null and differ from the tail for scatter reading/scatter writing.
        var next: Node? = null
        var prev: Node? = null

        val availableToRead: Int get() = write - read
        val availableToWrite: Int get() = arr.size - write

        inline val nextByte get() = arr[read]
    }

    // initially the same
    internal var head = Node()
    internal var tail = head

    public actual fun clear() {
        // clean up linked list to make the GC's life easier
        var lastNode: Node? = head
        while (lastNode != null) {
            val next = lastNode.next
            lastNode.next = null
            next?.prev = null
            lastNode = next
        }

        head = Node()
        tail = head
    }

    /**
     * Gets the number of bytes that are available to read from this buffer.
     */
    public actual fun availableBytes(): ULong {
        var count = 0UL
        var node: Node? = head
        while (node != null) {
            count += node.availableToRead.toULong()
            node = node.next
        }

        return count
    }

    private fun remainingInNode(): Int {
        return tail.availableToWrite
    }

    private fun remainingBytes(): ULong {
        var count = 0UL
        var node: Node? = tail
        while (node != null) {
            count += node.availableToWrite.toULong()
            node = node.next
        }

        return count
    }

    /**
     * Appends a new node; useful for pre-allocating.
     */
    private fun appendNode(node: Node) {
        if (tail.next != null) {
            // search through the list to add to the final one
            println("searching through linked list")
            var last = tail.next!!
            while (true) {
                val next = last.next
                if (next == null) break
                else last = next
            }

            last.next = node
        } else {
            tail.next = node
        }
    }

    /**
     * Advances the read cursor by the specified size.
     */
    private fun advanceRead(size: Int) {
        head.read += size

        // slightly different logic, as the head node drops off the back
        if (head.read >= head.arr.size) {
            val next = head.next ?: Node()
            // GC helper
            head.next = null
            next.prev = null
            head = next
        }
    }

    private fun putByte(byte: Byte) {
        tail.arr[tail.write] = byte
        advanceWrite()
    }

    private fun advanceWrite() {
        tail.write++

        // advance the linked list if needed
        if (tail.availableToWrite <= 0) {
            val next = tail.next ?: Node().also { tail.next = it; it.prev = tail }
            tail = next
        }
    }

    /**
     * Require at least the specified amount of bytes for reading.
     */
    private fun requireForReading(size: Long): Either<Unit, ResourceError> {
        // walk through all nodes and return early if we find that we do have enough
        // capacity. otherwise, return an EndOfFileError.
        var last: Node? = head
        var remaining = 0
        while (last != null) {
            // unwritten nodes in the chain
            if (last.availableToRead == 0) break
            remaining += last.availableToRead

            if (remaining >= size) {
                return Either.ok(Unit)
            }

            last = last.next
        }

        return if (remaining == 0) Either.err(EndOfFileError)
        else Either.err(NotEnoughDataError)
    }

    /**
     * Allocates at least the specified amount of bytes for scatter reading/writing.
     */
    internal fun allocate(size: ULong) {
        // avoid walking the tree if there's enough in this node
        if (remainingInNode().toULong() > size || remainingBytes() > size) return

        // otherwise, just allocate full extra blocks. this'll overshoot
        val blocks = (size + nodeSize.toULong() - 1UL).div(nodeSize.toULong())


        for (block in 0L until blocks.toLong()) {
            appendNode(Node())
        }
    }

    // == reading methods == //

    // TODO: These are terrible, byte-by-byte copy methods. Fix that to do proper copying.
    public actual fun readIntoUpto(
        outputBuffer: ByteArray,
        byteCount: UInt,
        bufferOffset: Int
    ): Either<ByteCountResult, Fail> {
        val available = min(availableBytes().toUInt(), byteCount)
        if (available <= 0U) return Either.err(EndOfFileError)

        // insanely slow byte-by-byte copy
        return outputBuffer.checkBuffers(byteCount, bufferOffset)
            .andThen {
                var copied = 0U
                while (copied < byteCount) {
                    outputBuffer[bufferOffset + copied.toInt()] = head.nextByte
                    copied++
                    advanceRead(1)
                }

                Either.ok(ByteCountResult(available))
            }
    }

    public actual fun readIntoUpto(
        outputBuffer: Buffer, byteCount: ULong
    ): Either<ByteCountResult, Fail> {
        val available = min(availableBytes(), byteCount)
        if (available <= 0U) return Either.err(EndOfFileError)
        outputBuffer.allocate(available)

        // again, insanely slow byte-by-byte copy
        var copied = 0UL
        while (copied < available) {
            outputBuffer.putByte(head.nextByte)
            advanceRead(1)
            copied += 1UL
        }

        // TODO: make this ulong (that requires other changes) so I don't want it in the commit
        return Either.ok(ByteCountResult(copied.toUInt()))
    }

    public actual fun writeAll(
        buffer: ByteArray, byteCount: UInt, bufferOffset: Int
    ): Either<Unit, Fail> {
        for (i in 0U until byteCount) {
            writeByte(buffer[bufferOffset + i.toInt()])
        }

        return Either.ok(Unit)
    }

    public actual fun writeAll(
        buffer: Buffer, byteCount: ULong
    ): Either<Unit, Fail> {
        var counter = 0UL
        val ref = IOResultRef()

        while (counter < byteCount) {
            val res = buffer.readByte(ref)
            if (res.isFailure) return res
            writeByte(ref.byteValue)
            counter++
        }

        return Either.ok(Unit)
    }

    public actual fun readByte(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(1).andThen {
            out.byteValue = head.nextByte
            advanceRead(1)
            Either.ok(Unit)
        }
    }

    /**
     * Reads a single short from this buffer into the provided [IOResultRef].
     */
    public actual fun readShort(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(2).andThen {
            var value = head.arr[head.read].toInt()
            advanceRead(1)

            value = (value.shl(8)).or(head.nextByte.toInt())
            advanceRead(1)

            out.value = value.toULong()

            Either.ok(Unit)
        }
    }

    /**
     * Reads a single little-endian short from this buffer into the provided [IOResultRef].
     */
    public actual fun readShortLE(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(2).andThen {
            var value = head.nextByte.toULong()
            advanceRead(1)

            value = value.or(head.nextByte.toULong().shl(8))
            advanceRead(1)

            out.value = value

            Either.ok(Unit)
        }
    }

    /**
     * Reads a single int from this buffer into the provided [IOResultRef].
     */
    public actual fun readInt(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(4).andThen {
            var value = 0
            repeat(4) {
                value = (value.shl(8)).or(head.nextByte.toInt())
                advanceRead(1)
            }

            out.value = value.toULong()

            Either.ok(Unit)
        }
    }

    /**
     * Reads a single little-endian int from this buffer into the provided [IOResultRef].
     */
    public actual fun readIntLE(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(4).andThen {
            var value = 0UL
            repeat(4) {
                value = value.or(head.nextByte.toULong().shl(8))
                advanceRead(1)
            }

            out.value = value

            Either.ok(Unit)
        }
    }

    /**
     * Reads a single long from this buffer into the provided [IOResultRef].
     */
    public actual fun readLong(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(8).andThen {
            var value = 0UL
            repeat(8) {
                value = (value.shl(8)).or(head.nextByte.toULong())
                advanceRead(1)
            }

            out.value = value

            Either.ok(Unit)
        }
    }

    /**
     * Reads a single little-endian long from this buffer into the provided [IOResultRef].
     */
    public actual fun readLongLE(out: IOResultRef): Either<Unit, Fail> {
        return requireForReading(8).andThen {
            var value = 0UL
            repeat(8) {
                value = value.or(head.nextByte.toULong().shl(8))
                advanceRead(1)
            }

            out.value = value

            Either.ok(Unit)
        }
    }


    // == writing methods == //

    /**
     * Writes a single byte to this buffer.
     */
    public actual fun writeByte(byte: Byte) {
        allocate(1U)
        tail.arr[tail.write] = byte
        advanceWrite()
    }

    public actual fun writeShort(short: Short) {
        allocate(2U)
        tail.arr[tail.write] = (short.toInt().shr(8).toByte())
        advanceWrite()
        tail.arr[tail.write] = (short.toByte())
        advanceWrite()
    }

    public actual fun writeShortLE(short: Short) {
        allocate(2U)
        tail.arr[tail.write] = (short.toByte())
        advanceWrite()
        tail.arr[tail.write] = (short.toInt().shr(8).toByte())
        advanceWrite()
    }

    public actual fun writeInt(int: Int) {
        allocate(4U)
        tail.arr[tail.write] = (int.shr(24).toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.shr(16).toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.shr(8).toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.toByte())
        advanceWrite()
    }

    public actual fun writeIntLE(int: Int) {
        allocate(4U)
        tail.arr[tail.write] = (int.toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.shr(8).toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.shr(16).toByte())
        advanceWrite()
        tail.arr[tail.write] = (int.shr(24).toByte())
        advanceWrite()
    }

    public actual fun writeLong(long: Long) {
        allocate(8U)
        tail.arr[tail.write] = (long.shr(56).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(48).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(40).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(32).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(24).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(16).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(8).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.toByte())
        advanceWrite()
    }

    public actual fun writeLongLE(long: Long) {
        allocate(8U)
        tail.arr[tail.write] = (long.toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(8).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(16).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(24).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(32).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(40).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(48).toByte())
        advanceWrite()
        tail.arr[tail.write] = (long.shr(56).toByte())
        advanceWrite()
    }

    public fun writeFrom(arr: ByteArray) {
        allocate(arr.size.toULong())

        for (b in arr) {
            tail.arr[tail.write] = b
            advanceWrite()
        }
    }
}
