/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.annotations.ProvisionalApi

// bleghhhhh... constants are diff. on platforms.
// but the windows constants suck so we just use unix ones lol!

/**
 * Enumeration over the known file types.
 */
public enum class FileType(internal val number: UShort) {
    /** A socket file. */
    SOCKET(0xC000U),
    /** A symbolic link to another file. */
    SYMBOLIC_LINK(0xA000U),
    /** A regular file. */
    REGULAR(0x8000U),
    /** A block device. */
    BLOCK_DEVICE(0x6000U),
    /** A directory. */
    DIRECTORY(0x4000U),
    /** A character device. */
    CHARACTER_DEVICE(0x2000U),
    /** A FIFO. */
    FIFO(0x1000U),
    ;

    public companion object {
        // .entries keep was merged, but apparently this doesn't exist?
        @ProvisionalApi
        public val _entries: Array<FileType> = FileType.values()

        public fun toSet(value: UShort): Set<FileType> {
            val items = mutableSetOf<FileType>()
            for (type in FileType._entries) {
                if (value.and(type.number) != (0U).toUShort()) {
                    items.add(type)
                }
            }

            return items
        }
    }
}

// todo: add the rest of these

public fun Set<FileType>.isRegularFile(): Boolean = contains(FileType.REGULAR)
public fun Set<FileType>.isDirectory(): Boolean = contains(FileType.DIRECTORY)
public fun Set<FileType>.isSymlink(): Boolean = contains(FileType.SYMBOLIC_LINK)
