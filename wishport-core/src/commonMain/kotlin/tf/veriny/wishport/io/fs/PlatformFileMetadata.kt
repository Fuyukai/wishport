/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

/**
 * The [FileMetadata] for files on the real system filesystem. The fields of this metadata are
 * platform-dependent.
 */
public expect class PlatformFileMetadata : FileMetadata
