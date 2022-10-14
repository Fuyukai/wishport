/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("ClassName")
class `Test LinkedDequeSet` {
    @Test
    fun `Test head and tail`() {
        val set = TaskList<Int>()
        set.append(1)
        set.append(2)
        set.append(3)

        assertEquals(1, set.removeFirst())
        assertEquals(3, set.removeLast())
        assertEquals(2, set.removeFirst())
        assertEquals(null, set.removeLast())
    }

    @Test
    fun `Test removing arbitrary items`() {
        val set = TaskList<Int>()
        set.append(1)
        set.append(2)
        set.append(3)

        set.removeTask(2)
        assertEquals(3, set.removeLast())
        assertEquals(1, set.removeLast())
    }
}
