/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import kotlinx.cinterop.*
import platform.extra.wp_sockaddr_in6
import platform.posix.*
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.InternalWishportError
import tf.veriny.wishport.io.Fd
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SocketHandle

@Unsafe
public actual fun makeSocket(
    family: SocketFamily,
    type: SocketType,
    protocol: SocketProtocol
): ResourceResult<SocketHandle> {
    val typeNum = type.number.or(SOCK_CLOEXEC)

    val s = socket(family.number, typeNum, protocol.number)
    return if (s < 0) {
        posix_errno().toSysResult()
    } else {
        Either.ok(Fd.get(s))
    }
}

// getsockopt/setsockopt is a particularly bastard API
// there's no easy way to do this withoutt duplication.

// le hardcoded
@Unsafe
public actual fun <T : Any> setSocketOption(
    handle: IOHandle,
    option: SocketOption<T>,
    value: T
): Either<Unit, ResourceError> = memScoped {
    val fd = handle.actualFd

    val res = when (option) {
        is BooleanSocketOption -> {
            val item = alloc<UIntVar>()
            item.value = if (value as Boolean) 1U else 0U
            setsockopt(
                fd, option.level, option.optname, item.ptr, sizeOf<UIntVar>().toUInt()
            )
        }
        is UIntSocketOption -> {
            val item = alloc<UIntVar>()
            item.value = value as UInt
            setsockopt(
                fd, option.level, option.optname, item.ptr, sizeOf<UIntVar>().toUInt()
            )
        }
    }

    return if (res < 0) posix_errno().toSysResult()
    else Either.ok(Unit)
}

@OptIn(ExperimentalUnsignedTypes::class)
@Unsafe
public actual fun <T : Any> getSocketOption(
    handle: IOHandle,
    option: SocketOption<T>
): Either<T, ResourceError> = memScoped {
    val fd = handle.actualFd

    when (option) {
        is BooleanSocketOption -> {
            val out = alloc<UIntVar>()
            val res = getsockopt(
                fd, option.level, option.optname, out.ptr, cValuesOf(sizeOf<UIntVar>().toUInt())
            )

            // obviously safe, but kotlin doesn't understand that T is a boolean
            @Suppress("UNCHECKED_CAST")
            return if (res < 0) posix_errno().toSysResult()
            else Either.ok((out.value != 0U) as T)
        }
        is UIntSocketOption -> {
            val out = alloc<UIntVar>()
            val res = getsockopt(
                fd, option.level, option.optname, out.ptr, cValuesOf(sizeOf<UIntVar>().toUInt())
            )

            // obviously safe, but kotlin doesn't understand that T is a UInt (again)
            @Suppress("UNCHECKED_CAST")
            return if (res < 0) posix_errno().toSysResult()
            else Either.ok(out.value as T)
        }
    }
}

// TODO: move these basic ones up into a posix module and only handle linux-specific ones here
@Unsafe
internal fun createCAddress(
    alloc: NativePlacement, address: SocketAddress
): Pair<CPointer<sockaddr>, Long> = with(alloc) {
    return when (address) {
        is Inet4SocketAddress -> {
            val addr = alloc<sockaddr_in>()
            memset(addr.ptr, 0, sizeOf<sockaddr_in>().toULong())
            addr.sin_family = AF_INET.toUShort()
            addr.sin_addr.s_addr = htonl(address.address.toUInt())
            addr.sin_port = htons(address.port)

            addr.ptr.reinterpret<sockaddr>() to sizeOf<sockaddr_in>()
        }
        is Inet6SocketAddress -> {
            val addr = alloc<wp_sockaddr_in6>()
            memset(addr.ptr, 0, sizeOf<wp_sockaddr_in6>().toULong())

            addr.sin6_family = AF_INET6.toUShort()
            address.address.representation.unwrap().usePinned {
                memcpy(addr.sin6_addr.addr, it.addressOf(0), 16)
            }

            addr.sin6_port = htons(address.port)

            addr.ptr.reinterpret<sockaddr>() to sizeOf<wp_sockaddr_in6>()
        }
    }
}

@Unsafe
internal fun createKotlinAddress(
    type: SocketType, protocol: SocketProtocol,
    address: CPointer<sockaddr_storage>
): SocketAddress {
    return when (val family = address.pointed.ss_family.toInt()) {
        SocketFamily.IPV4.number -> {
            val ptr = address.reinterpret<sockaddr_in>()
            val rawIp = ntohl(ptr.pointed.sin_addr.s_addr)
            val ip = IPv4Address.of(rawIp)
            val port = ntohs(ptr.pointed.sin_port)
            Inet4SocketAddress(type, protocol, ip, port)
        }
        SocketFamily.IPV6.number -> {
            val ptr = address.reinterpret<wp_sockaddr_in6>()
            val rawIp = ptr.pointed.sin6_addr.addr.readBytesFast(16)
            val ip = IPv6Address(rawIp)
            val port = ntohs(ptr.pointed.sin6_port)
            Inet6SocketAddress(type, protocol, ip, port)
        }
        else -> throw InternalWishportError("Unsupported socket type '$family'")
    }
}

@OptIn(Unsafe::class)
public actual fun getRemoteAddress(
    sock: SocketHandle,
    type: SocketType?,
    protocol: SocketProtocol?,
): ResourceResult<SocketAddress> = Imperatavize.either {
    memScoped {
        var realType = type
        if (realType == null) {
            val typeValue = getSocketOption(sock, SO_TYPE).q().toInt()
            realType = SocketType.values().find { it.number == typeValue }!!
        }

        var realProtocol = protocol
        if (realProtocol == null) {
            val protoValue = getSocketOption(sock, SO_PROTOCOL).q().toInt()
            realProtocol = SocketProtocol.values().find { it.number == protoValue }!!
        }

        val addr = alloc<sockaddr_storage>()
        val sizeOut = alloc<UIntVar>()
        sizeOut.value = sizeOf<sockaddr_storage>().toUInt()
        val res = getpeername(
            sock.actualFd, addr.ptr.reinterpret(), sizeOut.ptr
        )
        if (res < 0) {
            @Suppress("RemoveExplicitTypeArguments")
            res.toSysResult().q<Nothing, ResourceError>()
        } else {
            createKotlinAddress(realType, realProtocol, addr.ptr)
        }
    }
}

@OptIn(Unsafe::class)
public actual fun getLocalAddress(
    sock: SocketHandle,
    type: SocketType?,
    protocol: SocketProtocol?,
): ResourceResult<SocketAddress> = Imperatavize.either {
    memScoped {
        var realType = type
        if (realType == null) {
            val typeValue = getSocketOption(sock, SO_TYPE).q().toInt()
            realType = SocketType.values().find { it.number == typeValue }!!
        }

        var realProtocol = protocol
        if (realProtocol == null) {
            val protoValue = getSocketOption(sock, SO_PROTOCOL).q().toInt()
            realProtocol = SocketProtocol.values().find { it.number == protoValue }!!
        }

        val addr = alloc<sockaddr_storage>()
        val sizeOut = alloc<UIntVar>()
        sizeOut.value = sizeOf<sockaddr_storage>().toUInt()

        val res = getsockname(
            sock.actualFd, addr.ptr.reinterpret(), sizeOut.ptr
        )

        if (res < 0) {
            @Suppress("RemoveExplicitTypeArguments")
            res.toSysResult().q<Nothing, ResourceError>()
        } else {
            createKotlinAddress(realType, realProtocol, addr.ptr)
        }
    }
}

public actual fun listen(handle: IOHandle, backlog: Int): ResourceResult<Unit> {
    val res = listen(handle.actualFd, backlog)

    return if (res < 0) posix_errno().toSysResult()
    else Either.ok(Unit)
}
