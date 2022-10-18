package tf.veriny.wishport.io.fs

// TODO: linux-only sticky bits?

/**
 * Enumeration of the possible permission values when creating a fresh file.
 */
public enum class FilePermissions(public val posixNumber: Int) {
    OWNER_READ(256),  // 0o400
    OWNER_WRITE(128), // 0o200
    OWNER_EXEC(64),   // 0o100
    GROUP_READ(32),   // 0o040
    GROUP_WRITE(16),  // 0o020
    GROUP_EXEC(8),    // 0o010
    ANY_READ(4),      // 0o004
    ANY_WRITE(2),     // 0o002
    ANY_EXEC(1),      // 0o001

    ;

    public companion object {
        public val DEFAULT_FILE: Set<FilePermissions> = setOf(
            OWNER_READ, OWNER_WRITE,
            GROUP_READ,
            ANY_READ,
        )

        public val DEFAULT_DIRECTORY: Set<FilePermissions> = setOf(
            OWNER_READ, OWNER_WRITE, OWNER_EXEC,
            GROUP_READ, GROUP_EXEC,
            ANY_READ, ANY_EXEC
        )
    }
}

public fun Collection<FilePermissions>.toMode(): UInt {
    return map { it.posixNumber }.reduce { acc, i -> acc.or(i) }.toUInt()
}