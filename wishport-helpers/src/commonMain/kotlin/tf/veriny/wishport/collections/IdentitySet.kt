/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.collections

/**
 * Defines an ordered hash set that uses object identity rather than ``Any#hashCode`` for objects.
 * This is used for e.g. closing scopes which need to keep track of objects by their identity,
 * rather by their equality, as two different objects may hold different handles but may be
 * equal to each-other.
 *
 * Warning: This may box the values in a wrapper value. This should be avoided for
 * performance-sensitive code.
 */
public expect class IdentitySet<E> public constructor() : MutableSet<E>
