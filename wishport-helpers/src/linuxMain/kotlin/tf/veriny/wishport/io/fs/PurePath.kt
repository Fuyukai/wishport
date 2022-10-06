/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.b

public actual val PATH_SEP: ByteString = b("/")

public actual typealias SystemPurePath = PosixPurePath