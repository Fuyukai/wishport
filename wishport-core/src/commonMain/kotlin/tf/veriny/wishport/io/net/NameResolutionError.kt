/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.net

import tf.veriny.wishport.Fail
import tf.veriny.wishport.ResourceError

/**
 * Error hierarchy for name resolution errors.
 */
public sealed interface NameResolutionError : Fail

// EAI_ADDRFAMILY
/**
 * The specified network host does not have any network addresses in the requested address family.
 */
public object NotThisAddressFamily : NameResolutionError

// EAI_AGAIN
/**
 * The nameserver returned a temporary failure indication. Try again later.
 */
public object NameserverTemporaryFailure : NameResolutionError

/**
 * getaddrinfo()-specific failure.
 */
public object GAIBadFlags : NameResolutionError

/**
 * The nameserver returned a permanent failure indication.
 */
public object NameserverPermanentFailure : NameResolutionError

/**
 * getaddrinfo()-specific failure.
 */
public object GAIOutOfMemory : NameResolutionError

/**
 * The specified network host exists, but does not have any network addresses defined.
 */
public object NoNetworkAddresses : NameResolutionError

/**
 * The name or service of the requested address is not known.
 */
public object NameOrServiceNotKnown : NameResolutionError

/**
 * The requested service is not available for the requested socket type.
 */
public object ServiceUnavailable : NameResolutionError

/**
 * The specified socket type is not supported.
 */
public object UnsupportedSocketType : NameResolutionError

/**
 * getaddrinfo()-specific failure.
 */
public class GAISystemError(public val other: ResourceError) : NameResolutionError
