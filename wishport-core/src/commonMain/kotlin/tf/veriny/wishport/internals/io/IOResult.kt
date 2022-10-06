/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("CanSealedSubClassBeObject")

package tf.veriny.wishport.internals.io

import tf.veriny.wishport.Closeable

/**
 * A readable handle for usage in I/O methods.
 */
// marker interfaces
public expect interface ReadableHandle : Closeable

/**
 * Hierachy over the possible I/O results returned from an IO manager.
 */
public expect sealed interface IOResult

/**
 * A handle to an open directory on a filesystem. This may be a real directory on the default
 * filesystem or a directory in a customised filesystem.
 */
public expect class DirectoryHandle : IOResult, ReadableHandle

/**
 * A handle to an open file on a filesystem. This may be a real file on the default filesystem or a
 * file in a customised filesystem.
 */
public expect class RawFileHandle : IOResult, ReadableHandle

/**
 * The result of a read/write operation.
 */
public expect value class ByteCountResult(public val count: Int) : IOResult
