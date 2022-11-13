/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.expect

public actual val PATH_SEP: ByteString = b("/")

public actual typealias SystemPurePath = PosixPurePath

public actual fun systemPathFor(s: String): SystemPurePath =
    PosixPurePath.from(s).expect("how did you get a null byte in a String?")
public actual fun systemPathFor(bs: ByteString): PathResult<SystemPurePath> = PosixPurePath.from(bs)
