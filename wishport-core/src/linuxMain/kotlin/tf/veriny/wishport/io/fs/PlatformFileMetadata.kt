/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * Platform metadata for statx() data.
 */
public actual class PlatformFileMetadata(
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

    // raw params
    fileMode: UShort,
) : FileMetadata {
    /** The type of this file. */
    public override val type: Set<FileType> by lazy {
        val mode = fileMode.and(0xF000U)
        FileType.toSet(mode)
    }

    /** The set of permissions for this file. */
    public val permissions: Set<FilePermissions> by lazy {
        val perms = fileMode.and(0x1ffU)
        val items = mutableSetOf<FilePermissions>()
        for (type in FilePermissions._entries) {
            if (perms.and(type.posixNumber) != (0U).toUShort()) {
                items.add(type)
            }
        }

        items
    }
}
