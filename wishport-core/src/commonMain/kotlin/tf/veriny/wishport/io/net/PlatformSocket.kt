/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.Either
import tf.veriny.wishport.ResourceError
import tf.veriny.wishport.ResourceResult
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.io.IOHandle
import tf.veriny.wishport.io.SocketHandle

// These functions are internal and provide no guarantee of safety or stability. They can and will
// be removed at my whim.
// Use at your own risk.

// Unsafe: leaks fds
@Unsafe
@LowLevelApi
public expect fun makeSocket(
    family: SocketFamily,
    type: SocketType,
    protocol: SocketProtocol
): ResourceResult<SocketHandle>

// Unsafe: constructing a struct option with the wrong values can cause out of bound reads
@Unsafe
@LowLevelApi
public expect fun <T : Any> setSocketOption(
    handle: IOHandle,
    option: SocketOption<T>,
    value: T
): Either<Unit, ResourceError>

@Unsafe
@LowLevelApi
public expect fun <T : Any> getSocketOption(
    handle: IOHandle,
    option: SocketOption<T>
): Either<T, ResourceError>

@LowLevelApi
public expect fun listen(handle: IOHandle, backlog: Int): ResourceResult<Unit>
