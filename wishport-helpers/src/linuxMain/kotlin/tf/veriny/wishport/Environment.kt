/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.extra.secure_getenv
import platform.posix.posix_errno
import platform.posix.setenv
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.io.fs.PosixPurePath

@OptIn(Unsafe::class)
public actual fun getEnvironmentVariable(name: String, default: String?): String? {
    val result = secure_getenv(name)
    return result?.toKStringUtf8Fast() ?: default
}

@Unsafe
public actual fun setEnvironmentVariable(name: String, value: String) {
    setenv(name, value, 1)
}

public actual fun getWorkingDirectory(): ResourceResult<PosixPurePath> {
    var size = 64

    while (true) {
        val buffer = ByteArray(size)
        val res = buffer.usePinned {
            platform.posix.getcwd(it.addressOf(0), size.toULong())
        }

        return if (res != null) {
            val nullTerminated = buffer.indexOf(0)
            val bs = ByteString(buffer.copyOfRange(0, nullTerminated))
            // safe get(), as getWorkingDirectory won't give us a null byte.
            // if it does, that is a panic-worthy bug.
            Either.ok(
                PosixPurePath.from(bs).get()
                ?: error("getcwd() returned the invalid path $bs!")
            )
        } else {
            val errno = posix_errno()

            if (errno == ERANGE) {
                size *= 2
                continue
            } else {
                errno.toSysResult()
            }
        }
    }
}
