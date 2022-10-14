/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import platform.posix.SOCK_DGRAM
import platform.posix.SOCK_RAW
import platform.posix.SOCK_STREAM

public actual enum class SocketType(public actual val number: Int) {
    STREAM(SOCK_STREAM),
    DGRAM(SOCK_DGRAM),
    RAW(SOCK_RAW),
    ;
}
