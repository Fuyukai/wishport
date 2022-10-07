/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.fs.SystemPurePath

// https://github.com/rust-lang/rust/issues/90308

/**
 * Gets an environment variable by [name], or returns a [default] value.
 */
public expect fun getEnvironmentVariable(name: String, default: String? = null): String?

/**
 * Sets an environment variable by [name] to [value]. This function is marked unsafe as it is not
 * safe for concurrent accesses of the environment variables.
 */
@Unsafe
public expect fun setEnvironmentVariable(name: String, value: String)

/**
 * Gets the current working directory.
 */
public expect fun getWorkingDirectory(): ResourceResult<SystemPurePath>
