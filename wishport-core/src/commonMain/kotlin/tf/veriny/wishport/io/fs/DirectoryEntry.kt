/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.ByteString

/**
 * Wraps data about a single file in a directory.
 */
public data class DirectoryEntry(
    public val type: Set<FileType>,
    public val fileName: ByteString,
)
