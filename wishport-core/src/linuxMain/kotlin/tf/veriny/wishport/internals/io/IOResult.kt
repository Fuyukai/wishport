/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals.io

// see the expect definitions

public actual sealed interface IOResult

public actual object Empty : IOResult

public actual interface IOHandle {
    public val actualFd: Int
}

public class Fd(public override val actualFd: Int) : IOResult, IOHandle

public actual typealias DirectoryHandle = Fd
public actual typealias RawFileHandle = Fd

public actual value class ByteCountResult(public val count: Int) : IOResult
