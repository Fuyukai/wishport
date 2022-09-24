/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

/**
 * Gets a monotonically increasing counter in nanoseconds, plus a random offset.
 */
public expect fun getMonotonicTime(): Long

// test
public expect fun nanosleep(ns: Long)
