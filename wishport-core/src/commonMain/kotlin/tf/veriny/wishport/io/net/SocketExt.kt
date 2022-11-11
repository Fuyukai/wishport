/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe

/**
 * Accepts an incoming [Socket] connection, and adds it to the specified [AsyncClosingScope].
 */
@OptIn(Unsafe::class)
public suspend fun Socket.acceptInto(scope: AsyncClosingScope): CancellableResourceResult<Socket> {
    return accept().andAddTo(scope)
}
