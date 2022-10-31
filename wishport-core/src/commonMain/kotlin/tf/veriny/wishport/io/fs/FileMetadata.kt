/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * A filesystem-agnostic mechanism for wrapping file metadata.
 */
public interface FileMetadata {
    /** The size of this file, in bytes. */
    public val size: ULong

    /** The creation time, in positive nanoseconds. */
    public val creationTime: ULong

    /** The modification time, in positive nanoseconds. */
    public val modificationTime: ULong
}
