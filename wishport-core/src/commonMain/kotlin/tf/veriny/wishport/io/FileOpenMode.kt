/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

/**
 * Enumeration of possible ways to open a file.
 */
public enum class FileOpenMode {
    /**
     * The file will be opened in read-only mode.
     */
    READ_ONLY,

    /**
     * The file will be opened in write-only mode.
     */
    WRITE_ONLY,

    /**
     * The file will be opened for both reading and writing.
     */
    READ_WRITE,

    ;
}
