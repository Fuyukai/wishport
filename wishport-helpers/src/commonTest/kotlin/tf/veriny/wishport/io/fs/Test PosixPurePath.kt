/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.collections.b
import tf.veriny.wishport.get
import tf.veriny.wishport.isSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class `Test PosixPurePath` {
    @Test
    public fun `Test parsing absolute paths`() {
        val path = PosixPurePath.from("/var/lib")
        assertTrue(path.isSuccess)
        assertEquals(
            listOf(
                PathComponent.RootDir,
                PathComponent.Normal(b("var")),
                PathComponent.Normal(b("lib"))
            ),
            path.get()?.components
        )
        assertTrue(path.get()!!.isAbsolute)
    }

    @Test
    fun `Test parsing relative paths`() {
        val path = PosixPurePath.from("tmp/abc")
        assertTrue(path.isSuccess)
        assertEquals(
            listOf(
                PathComponent.Normal(b("tmp")),
                PathComponent.Normal(b("abc"))
            ),
            path.get()?.components
        )
    }

    @Test
    fun `Test parsing dots`() {
        val path = PosixPurePath.from("abc/./../xyz")
        assertTrue(path.isSuccess)
        assertEquals(
            listOf(
                PathComponent.Normal(b("abc")),
                PathComponent.CurrentDir,
                PathComponent.PreviousDir,
                PathComponent.Normal(b("xyz"))
            ),
            path.get()?.components
        )
    }

    @Test
    fun `Test parsing extra slashes`() {
        val path = PosixPurePath.from("test///1///2")
        assertTrue(path.isSuccess)
        assertEquals(3, path.get()!!.components.size)
    }

    @Test
    fun `Test parent`() {
        val path = PosixPurePath.from("/a/b/c").get()!!
        assertEquals(b("/a/b"), path.parent?.toByteString(), "parent should be /a/b")

        assertEquals(b("/a"), path.parent?.parent?.toByteString(), "parent should be just /a")
    }

    @Test
    fun `Test ancestors`() {
        val path = PosixPurePath.from("a/b/c/d").get()!!
        val ancestors = path.ancestors
        assertEquals(3, ancestors.size, "path should have 3 ancestors")
        // make sure the order is rigt
        assertEquals(b("a/b/c"), ancestors.first().toByteString())
        assertEquals(b("a"), ancestors.last().toByteString())
    }
}
