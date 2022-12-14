/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.AsyncClosingScope
import tf.veriny.wishport.CancellableResult
import tf.veriny.wishport.Fail
import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * Opens a new temporary file in an available temporary directory, adds it to the specified
 * [scope], and returns a handle to the file.
 */
@ProvisionalApi
public expect suspend fun openTemporaryFile(
    scope: AsyncClosingScope,
    mode: FileOpenType = FileOpenType.READ_WRITE,
    flags: Set<FileOpenFlags> = setOf()
): CancellableResult<BufferedFile, Fail>

/**
 * Creates a new temporary directory and invokes the specified [block] with it.
 */
@ProvisionalApi
public expect suspend fun <S, F : Fail> createTemporaryDirectory(
    block: suspend (SystemFilesystemHandle) -> CancellableResult<S, F>
): CancellableResult<S, Fail>
