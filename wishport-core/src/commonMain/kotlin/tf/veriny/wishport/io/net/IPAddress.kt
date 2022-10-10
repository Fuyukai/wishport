package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString

// see: https://docs.python.org/3/library/ipaddress.html
// see: https://doc.rust-lang.org/std/net/enum.IpAddr.html
// and of course, JDK InetAddress

/**
 * Abstract sealed superclass for all IP addresses.
 */
public sealed class IPAddress {
    public companion object {
        public const val IP_VERSION_4: Int = 4
        public const val IP_VERSION_6: Int = 6
    }

    /** The raw bytestring representation of this address. */
    public abstract val representation: ByteString

    /** The version number for this address (e.g. 4 for IPv4, 6 for IPv6) */
    public abstract val version: Int

    /** The address family for this IP address. */
    public abstract val family: SocketFamily

    // TODO: Other attributes that other languages have but aren't needed for a prototype right now.
}

// both of these classes contain their IP address in network order bytearrays
/**
 * An IP address using version 4.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public class IPv4Address(bytes: ByteArray) : IPAddress() {
    public companion object {
        /**
         * Parses an IPv4 address from a String.
         */
        public fun of(ip: String): Either<IPv4Address, IPAddressParseFailure> {
            val split = ip.split('.')
            if (split.size != 4) {
                return Either.err(
                    IPAddressParseFailure("Address should have 4 octets", ip)
                )
            }
            if (split.any { it.isEmpty() }) {
                return Either.err(
                    IPAddressParseFailure("Address must not have empty octets", ip)
                )
            }

            val ints = split.map { it.toInt() }
            if (ints.any { it < 0 || it > 255 }) {
                return Either.err(
                    IPAddressParseFailure("Octets must be in [0,255] range", ip)
                )
            }

            return Either.ok(IPv4Address(ints.map { it.toByte() }.toByteArray()))
        }

        /**
         * Parses an IPv4 address from a decimal [UInt].
         */
        @OptIn(ExperimentalUnsignedTypes::class)
        public fun of(decimal: UInt): IPv4Address {
            return IPv4Address(decimal.toByteArray())
        }
    }

    override val representation: ByteString = ByteString(bytes)

    override val version: Int = IP_VERSION_4
    override val family: SocketFamily get() = SocketFamily.IPV4

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IPv4Address) return false
        return other.representation == representation
    }

    override fun hashCode(): Int {
        return representation.hashCode()
    }

    override fun toString(): String {
        return representation.joinToString(".") { it.toUByte().toString() }
    }
}

/**
 * An IP address using version 6.
 */
public class IPv6Address(bytes: ByteArray) : IPAddress() {
    public companion object {
        /**
         * Parses an IPv6 address from a String.
         */
        public fun of(ip: String): Either<IPv6Address, IPAddressParseFailure> {
            return IPv6TextParser.parse(ip).andThen { Either.ok(IPv6Address(it)) }
        }
    }

    @OptIn(Unsafe::class)
    override val representation: ByteString = ByteString(bytes)

    override val version: Int = IP_VERSION_6
    override val family: SocketFamily get() = SocketFamily.IPV6

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IPv6Address) return false
        return other.representation == representation
    }

    override fun hashCode(): Int {
        return representation.hashCode()
    }

    @OptIn(Unsafe::class)
    override fun toString(): String {
        return IPv6Stringifier(representation.unwrap()).correct()
    }
}

/**
 * Returned when an IP address fails to parse.
 */
public class IPAddressParseFailure(public val why: String, public val addres: String) : Fail