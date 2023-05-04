package tf.veriny.wishport.io

import tf.veriny.wishport.Either
import tf.veriny.wishport.Fail

/**
 * An in-memory, automatically growable buffer of binary data.
 */
public expect class Buffer {
    /** Gets the number of available bytes that have been written to this buffer. */
    public fun availableBytes(): ULong

    /**
     * Clears this buffer entirely,
     */
    public fun clear()

    // == generic methods == //
    /**
     * Reads out [byteCount] bytes into the provided [outputBuffer].
     */
    public fun readIntoUpto(
        outputBuffer: ByteArray,
        byteCount: UInt = outputBuffer.size.toUInt(),
        bufferOffset: Int = 0
    ): Either<ByteCountResult, Fail>

    /**
     * Reads out upto [byteCount] bytes into the provided [outputBuffer].
     */
    public fun readIntoUpto(
        outputBuffer: Buffer, byteCount: ULong = this.availableBytes()
    ): Either<ByteCountResult, Fail>

    /**
     * Writes all data from the provided [buffer] into this one.
     */
    public fun writeAll(
        buffer: ByteArray, byteCount: UInt = buffer.size.toUInt(), bufferOffset: Int = 0
    ): Either<Unit, Fail>

    /**
     * Writes all data from the provided [buffer] into this one.
     */
    public fun writeAll(
        buffer: Buffer, byteCount: ULong = buffer.availableBytes()
    ): Either<Unit, Fail>

    // convenience read methods
    /**
     * Reads a single byte from this buffer into the provided [IOResultRef].
     */
    public fun readByte(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single short from this buffer into the provided [IOResultRef].
     */
    public fun readShort(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single little-endian short from this buffer into the provided [IOResultRef].
     */
    public fun readShortLE(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single int from this buffer into the provided [IOResultRef].
     */
    public fun readInt(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single little-endian int from this buffer into the provided [IOResultRef].
     */
    public fun readIntLE(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single long from this buffer into the provided [IOResultRef].
     */
    public fun readLong(out: IOResultRef): Either<Unit, Fail>

    /**
     * Reads a single little-endian long from this buffer into the provided [IOResultRef].
     */
    public fun readLongLE(out: IOResultRef): Either<Unit, Fail>

    // convenience write methods
    /**
     * Writes a single byte to this buffer.
     */
    public fun writeByte(byte: Byte)

    /**
     * Writes a single short to this buffer.
     */
    public fun writeShort(short: Short)

    /**
     * Writes a single little-endian short to this buffer.
     */
    public fun writeShortLE(short: Short)

    /**
     * Writes a single int to this buffer.
     */
    public fun writeInt(int: Int)

    /**
     * Writes a single little-endian int to this buffer.
     */
    public fun writeIntLE(int: Int)

    /**
     * Writes a single long to this buffer.
     */
    public fun writeLong(long: Long)

    /**
     * Writes a single little-endian long to this buffer.
     */
    public fun writeLongLE(long: Long)
}
