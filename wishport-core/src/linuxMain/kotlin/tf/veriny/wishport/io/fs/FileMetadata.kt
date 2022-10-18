package tf.veriny.wishport.io.fs

public actual data class FileMetadata(
    override val fileSize: ULong,
    override val creationTime: ULong,
    override val modificationTime: ULong,
    override val linkCount: UInt,
    /** The user ID of the owner of this file. */
    public val ownerUid: UInt,
    /** The group ID that owns this file. */
    public val ownerGid: UInt,
) : CommonMetadata(fileSize, creationTime, modificationTime, linkCount)
