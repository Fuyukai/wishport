/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import kotlinx.cinterop.*
import platform.posix.*
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
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

public actual fun listen(handle: IOHandle, backlog: Int): ResourceResult<Unit> {
    val res = listen(handle.actualFd, backlog)

    return if (res < 0) posix_errno().toSysResult()
    else Either.ok(Unit)
}
