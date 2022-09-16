/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.collections

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Makes sure the priority queue stuff works
 */
public class `Test PriorityQueue` {
    @Test
    public fun `Test adding and removing`() {
        // don't do this with ints in reality!!
        val q = PriorityQueue<Int>()

        q.add(4)
        q.add(27)
        q.add(17)
        q.add(9)

        assertEquals(4, q.remove())
        assertEquals(9, q.remove())
        assertEquals(17, q.remove())

        q.add(15)

        assertEquals(15, q.remove())
        assertEquals(27, q.remove())
    }
}
