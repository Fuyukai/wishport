/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.io.writeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ProvisionalApi::class)
class `Test Filesystem IO` {
    @Test
    fun `Test creating a fresh directory`() = runUntilCompleteNoResult {
        val path = systemPathFor("test")

        assertSuccess {
            createTemporaryDirectory {
                Imperatavize.cancellable {
                    it.createDirectoryRelative(path).q()
                    val metadata = it.metadataFor(path).q()
                    assertTrue(metadata.type.isDirectory())
                }
            }
        }
    }

    @Test
    fun `Test creating an existing directory`() = runUntilCompleteNoResult {
        val path = systemPathFor("test")

        assertFailureWith(FileExists) {
            createTemporaryDirectory {
                repeatedly {
                    it.createDirectoryRelative(path)
                }
            }
        }
    }

    @Test
    fun `Test removing directories`() = runUntilCompleteNoResult {
        val path = systemPathFor("test")
        val notExisting = systemPathFor("not-existing")

        assertSuccess {
            createTemporaryDirectory {
                it.createDirectoryRelative(path)
                    .andThen { _ -> it.removeRelative(path, isDirectory = true) }
            }
        }

        assertFailureWith(NoSuchFileOrDirectory) {
            createTemporaryDirectory {
                it.removeRelative(notExisting, isDirectory = true)
            }
        }
    }

    @Test
    fun `Test listing directories`() = runUntilCompleteNoResult {
        assertSuccess {
            createTemporaryDirectory {
                AsyncClosingScope { scope ->
                    Imperatavize.cancellable {
                        it.openBufferedRelative(
                            scope,
                            systemPathFor("test"), FileOpenType.WRITE_ONLY,
                            setOf(FileOpenFlags.CREATE_IF_NOT_EXISTS)
                        )
                            .andAlso { it.writeAll(b("test")) }
                            .andThen { it.close() }
                            .q()

                        val contents = it.listDirectory().q()
                        assertEquals(1, contents.size)
                        assertEquals(b("test"), contents.first().fileName)
                    }
                }
            }
        }
    }
}
