@file:Suppress("ClassName")

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.io.seekFromStart
import tf.veriny.wishport.io.streams.readUntilEof
import tf.veriny.wishport.io.streams.writeAll
import kotlin.test.Test
import kotlin.test.assertContentEquals

@OptIn(ProvisionalApi::class)
class `Test FileLikeHandle helpers` {
    private val DATA = "DATA1234".encodeToByteArray()

    @Test
    fun `Test write + read all`() = runUntilCompleteNoResult {
        assertSuccess {
            AsyncClosingScope.withImperatavize<Unit, Fail> { scope ->
                createTemporaryDirectory { dir ->
                    val file =
                        dir.getFileHandleRelative(
                            scope,
                            systemPathFor("test"),
                            FileOpenType.READ_WRITE,
                            setOf(FileOpenFlags.MUST_CREATE)
                        ).q()

                    file.writeAll(DATA).q()

                    // test with default sized chunk
                    file.seekFromStart(0).q()
                    assertContentEquals(DATA, file.readUntilEof().q())

                    // test with explicit chunk
                    file.seekFromStart(0).q()
                    assertContentEquals(DATA, file.readUntilEof(DATA.size.toUInt()).q())

                    // test with non-multiple chunk
                    file.seekFromStart(0).q()
                    assertContentEquals(DATA, file.readUntilEof(3U).q())

                    Cancellable.empty()
                }
            }
        }
    }
}