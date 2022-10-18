/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

// see: _BY_HANDLE_FILE_INFORMATION, statx(2)

public abstract class CommonMetadata(
    /** The file size, in bytes. */
    public open val fileSize: ULong,
    /** The creation time, in positive nanoseconds. */
    public open val creationTime: ULong,
    /** The modification time, in positive nanoseconds. */
    public open val modificationTime: ULong,
    /** The number of hard links to this file. */
    public open val linkCount: UInt,
)

/**
 * Wraps the possible metadata for this file. This metadata is platform-specific, but some fields
 * are exposed on all platforms.
 */
public expect class FileMetadata : CommonMetadata
