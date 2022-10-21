/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

public actual data class PlatformFileMetadata(
    override val size: ULong,
    override val creationTime: ULong,
    override val modificationTime: ULong,
    /** The number of hard links to this file. */
    public val linkCount: UInt,
    /** The user ID of the owner of this file. */
    public val ownerUid: UInt,
    /** The group ID that owns this file. */
    public val ownerGid: UInt,
    /** The block size for the filesystem that this file resides on. */
    public val blockSize: UInt,
) : FileMetadata
