/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io

import tf.veriny.wishport.Cancellable

/**
 * A readable handle for usage in I/O methods.
 */
// marker interfaces
public expect interface IOHandle

/**
 * Hierachy over the possible I/O results returned from an IO manager.
 */
public expect sealed interface IOResult

/**
 * An empty result for system calls that don't return anything.
 */
public object Empty : IOResult {
    internal val RESULT = Cancellable.ok(Empty)
}

/**
 * A handle to an open socket.
 */
public expect class SocketHandle : IOResult, IOHandle

/**
 * A handle to an open directory on a filesystem. This may be a real directory on the default
 * filesystem or a directory in a customised filesystem.
 */
public expect class DirectoryHandle : IOResult, IOHandle

/**
 * A handle to an open file on a filesystem. This may be a real file on the default filesystem or a
 * file in a customised filesystem.
 */
public expect class RawFileHandle : IOResult, IOHandle

/**
 * The result of a read/write operation.
 */
public value class ByteCountResult(public val count: UInt) : IOResult

/**
 * The result of a poll operation.
 */
public value class PollResult(public val polled: Set<Poll>) : IOResult

/**
 * The result of a seek operation.
 */
public value class SeekPosition(public val position: Long) : IOResult
