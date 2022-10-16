/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.CancellableResult

/**
 * Defines a way to resolve user-readable hostnames (e.g. ``one.one.one.one``) into IP addresses
 * for connecting over the internet (e.g. ``2606:4700:4700::1111``).
 */
public interface NameResolver {
    /**
     * Gets an [EndpointInfo] from the specified [hostname]:[port] combination, for a socket of
     * [socketType] type.
     *
     * If [socketFamily] or [socketProtocol] are passed, these will be used as hints to the
     * underlying functionality; they may be ignored. Otherwise, this will return all available
     * socket families and protocols.
     */
    public suspend fun <T : EndpointInfo> getAddressFromName(
        hostname: String,
        port: Int,
        socketType: SocketType,
        socketFamily: SocketFamily? = null,
        socketProtocol: SocketProtocol? = null,
    ): CancellableResult<T, NameResolutionError>
}
