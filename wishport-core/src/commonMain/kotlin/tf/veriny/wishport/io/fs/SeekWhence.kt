/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * Enumeration over the possible mechanisms for seeking a file.
 */
public enum class SeekWhence(public val value: Int) {
    /** Sets the current file pointer to a direct value. */
    SEEK_SET(0),
    /** Sets the current file pointer to the current pointer plus a provided value. */
    SEEK_CURRENT(1),
    /** Sets the current file pointer to the file size plus a provided value. */
    SEEK_END(2),

    ;
}
