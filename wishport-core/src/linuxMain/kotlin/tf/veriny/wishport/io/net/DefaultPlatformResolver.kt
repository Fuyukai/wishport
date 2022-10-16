/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import kotlinx.cinterop.*
import platform.extra._EAI_ADDRFAMILY
import platform.extra._EAI_NODATA
import platform.posix.*
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe

public actual object DefaultPlatformResolver : NameResolver {
    private fun toGAIError(errno: Int): NameResolutionError {
        return when (errno) {
            _EAI_ADDRFAMILY, EAI_FAMILY -> NotThisAddressFamily
            EAI_AGAIN -> NameserverTemporaryFailure
            EAI_BADFLAGS -> GAIBadFlags
            EAI_FAIL -> NameserverPermanentFailure
            EAI_MEMORY -> GAIOutOfMemory
            _EAI_NODATA -> NoNetworkAddresses
            EAI_NONAME -> NameOrServiceNotKnown
            EAI_SERVICE -> ServiceUnavailable
            EAI_SOCKTYPE -> UnsupportedSocketType
            EAI_SYSTEM -> GAISystemError(posix_errno().toSysError())
            else -> error("gai returned unknown error $errno")
        }
    }

    // threading shenanigans
    private class GAI(
        val hostname: String,
        val port: Int,
        val socketType: SocketType,
        val socketFamily: SocketFamily?,
        val socketProtocol: SocketProtocol?
    )

    // ran inside a separate thread
    @OptIn(Unsafe::class)
    private fun <T : EndpointInfo> getaddrinfo(gai: GAI): Either<T, NameResolutionError> = memScoped {
        val hints = alloc<addrinfo>()
        memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
        val out = allocPointerTo<addrinfo>()

        // apparently u should do this
        hints.ai_canonname = null
        hints.ai_addr = null
        hints.ai_next = null

        hints.ai_socktype = gai.socketType.number
        if (gai.socketFamily != null) {
            hints.ai_family = gai.socketFamily.number
        } else {
            hints.ai_family = PF_UNSPEC
        }
        if (gai.socketProtocol != null) {
            hints.ai_protocol = gai.socketProtocol.number
        }

        hints.ai_flags = flags(AI_ADDRCONFIG, AI_V4MAPPED)

        val res = getaddrinfo(
            gai.hostname, gai.port.toString(), hints.ptr, out.ptr
        )

        if (res != 0) {
            return Either.err(toGAIError(res))
        }

        // traverse the list of entries and convert them to our own
        var nextPtr = out.value
        val addresses = mutableSetOf<SocketAddress>()
        while (true) {
            if (nextPtr?.pointed == null) break
            val nextEntry = nextPtr.pointed

            assert(nextEntry.ai_addr != null)

            val proto = SocketProtocol.values().find { it.number == nextEntry.ai_protocol }
                ?: error("invalid socket protocol???")

            val type = SocketType.values().find { it.number == nextEntry.ai_socktype }
                ?: error("invalid socket type???")

            assert(type == gai.socketType)

            val sa = when (nextEntry.ai_family) {
                SocketFamily.IPV4.number -> {
                    assert(nextEntry.ai_addrlen == sizeOf<sockaddr_in>().toUInt())
                    val addr = nextEntry.ai_addr!!.reinterpret<sockaddr_in>()
                    val ip = IPv4Address(ntohl(addr.pointed.sin_addr.s_addr).toByteArray())
                    Inet4SocketAddress(proto, type, ip, addr.pointed.sin_port.convert())
                }
                SocketFamily.IPV6.number -> {
                    assert(nextEntry.ai_addrlen == sizeOf<sockaddr_in6>().toUInt())
                    val addr = nextEntry.ai_addr!!.reinterpret<sockaddr_in6>()
                    // XXX: kotlin sin6_addr doesn't have any members...
                    val ipBytes = addr.pointed.sin6_addr.arrayMemberAt<ByteVar>(0)
                    val ip = IPv6Address(ipBytes.readBytesFast(16))
                    Inet6SocketAddress(proto, type, ip, addr.pointed.sin6_port.convert())
                }
                else -> null
            }

            sa?.let { addresses.add(it) }
            nextPtr = nextEntry.ai_next
        }

        if (out.value != null) {
            freeaddrinfo(out.value)
        }

        return Either.ok(
            when (gai.socketType) {
                SocketType.STREAM -> { StreamEndpointInfo(addresses) }
                SocketType.DGRAM -> { DatagramEndpointInfo(addresses) }
                SocketType.RAW -> { RawEndpointInfo(addresses) }
            }
        ) as Either<T, NameResolutionError>
    }

    override suspend fun <T : EndpointInfo> getAddressFromName(
        hostname: String,
        port: Int,
        socketType: SocketType,
        socketFamily: SocketFamily?,
        socketProtocol: SocketProtocol?
    ): CancellableResult<T, NameResolutionError> {
        return runSynchronouslyOffThread(
            { GAI(hostname, port, socketType, socketFamily, socketProtocol) },
            true,
        ) { getaddrinfo(it) }
    }
}
