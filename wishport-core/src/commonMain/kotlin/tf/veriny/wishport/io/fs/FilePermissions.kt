/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

// TODO: linux-only sticky bits?

/**
 * Enumeration of the possible permission values when creating a fresh file.
 */
public enum class FilePermissions(public val posixNumber: UShort) {
    OWNER_READ(256U), // 0o400
    OWNER_WRITE(128U), // 0o200
    OWNER_EXEC(64U), // 0o100
    GROUP_READ(32U), // 0o040
    GROUP_WRITE(16U), // 0o020
    GROUP_EXEC(8U), // 0o010
    ANY_READ(4U), // 0o004
    ANY_WRITE(2U), // 0o002
    ANY_EXEC(1U), // 0o001

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
    if (isEmpty()) return 0U

    return map { it.posixNumber }.reduce { acc, i -> acc.or(i) }.toUInt()
}
