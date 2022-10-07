/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import platform.extra.secure_getenv
import platform.posix.setenv
import tf.veriny.wishport.annotations.Unsafe

@OptIn(Unsafe::class)
public actual fun getEnvironmentVariable(name: String, default: String?): String? {
    val result = secure_getenv(name)
    return result?.toKStringUtf8Fast() ?: default
}

@Unsafe
public actual fun setEnvironmentVariable(name: String, value: String) {
    setenv(name, value, 1)
}
