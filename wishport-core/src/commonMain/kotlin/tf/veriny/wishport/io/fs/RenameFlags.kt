/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * A set of flags for the file rename operation.
 */
public enum class RenameFlags {
    /**
     * Does a best attempt at atomically exchanging the two files provided. This may not be
     * supported depending on your platform.
     */
    EXCHANGE,

    /**
     * If the destination path exists, then the rename will fail with EEXIST.
     */
    DONT_REPLACE,

    ;
}
