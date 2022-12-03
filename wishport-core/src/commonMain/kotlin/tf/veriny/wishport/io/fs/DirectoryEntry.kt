package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.ByteString

/**
 * Wraps data about a single file in a directory.
 */
public data class DirectoryEntry(
    public val type: Set<FileType>,
    public val fileName: ByteString,
)