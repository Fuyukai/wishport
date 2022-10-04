/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

import tf.veriny.wishport.Closeable

// see the expect definitions

public actual sealed interface IOResult

public class Fd(public val actualFd: Int) : Closeable, IOResult {
    private var closed = false

    override fun close() {
        if (closed) return

        platform.posix.close(actualFd)
        closed = true
    }
}

public actual typealias DirectoryHandle = Fd
public actual typealias FileHandle = Fd

public actual value class ByteCountResult(public val count: Int) : IOResult
